from fastapi.middleware.cors import CORSMiddleware
from fastapi import FastAPI, HTTPException
from fastapi.responses import FileResponse
from pathlib import Path
from typing import List
from enum import Enum
import requests
import uvicorn

app = FastAPI()


app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/stations")
async def find_city_stations_ids(city: str):
    url = f'https://api.gios.gov.pl/pjp-api/v1/rest/station/findAll?size=500'
    
    response = requests.get(url)
    
    if response.status_code != 200:
        raise HTTPException(status_code=500, detail="Couldn't get stations data")
    
    stations = response.json()

    city_stations_ids = []
    for station in stations["Lista stacji pomiarowych"]:
        if station["Nazwa miasta"] == city:
            city_stations_ids.append(station["Identyfikator stacji"])

    if not city_stations_ids:
        raise HTTPException(status_code=404, detail="Brak danych o danym mieście")

    return city_stations_ids

@app.get("/sensors")
async def find_station_sensors(station_id: int):
    url = f'https://api.gios.gov.pl/pjp-api/v1/rest/station/sensors/{station_id}'

    response = requests.get(url)

    if response.status_code != 200:
        raise HTTPException(status_code=500, detail="Couldn't get station sensors")
    
    sensors = response.json()

    return sensors["Lista stanowisk pomiarowych dla podanej stacji"]


class SensorType(str, Enum):
    SO2 = "dwutlenek siarki" #1
    O3 = "ozon" #5
    NO2 = "dwutlenek azotu" #6
    NOx = "tlenki azotu" #7
    CO = "tlenek wegla" #8
    C6H6 = "benzen" #10
    NO = "tlenek azotu" #16

    @classmethod
    def get_id(cls, sensor_name: str) -> int:
        sensor_map = {
            "dwutlenek siarki": 1,
            "ozon": 5,
            "tlenki azotu": 7,
            "tlenek wegla": 8,
            "benzen": 10,
            "tlenek azotu": 16
        }
        return sensor_map.get(sensor_name)
    

@app.get("/sensor-data")
async def get_sensor_data(sensor_id: int):

    # print("sensor_id: ", str(sensor_id))
    url = f'https://api.gios.gov.pl/pjp-api/v1/rest/data/getData/{sensor_id}'

    response = requests.get(url)
    
    if response.status_code != 200:
        raise HTTPException(status_code=500, detail=f"Couldn't get sensors data:")
    
    data = response.json()

    for sample in data["Lista danych pomiarowych"]:
        if sample["Wartość"] is not None:
            return sample
    
    return []
    

@app.get("/city-data")
async def get_city_data(city: str, sensor_type: SensorType):

    city_data = []

    #znajdujemy wszystkie stacje w miescie 
    stations_ids = await find_city_stations_ids(city)


    #dla kazdej stacji
    for station_id in stations_ids:
        #znajdujemy wszystkie sensory
        sensors = await find_station_sensors(station_id)
        
        #przchodzimy po sensorach
        for sensor in sensors:
            if sensor["Id wskaźnika"] == SensorType.get_id(sensor_type.value):
                sensor_data = await get_sensor_data(sensor["Identyfikator stanowiska"])
                if sensor_data:
                    city_data.append(sensor_data)

    return city_data
    
@app.get("/mean-value")
async def get_mean_value(city: str, sensor_type: SensorType):
    
    try:
        data = await get_city_data(city, sensor_type)
    except HTTPException as e:
        if e.status_code == 404:
            raise HTTPException(status_code=404, detail="Brak danych o danym mieście")

    mean_value = handle_data(data)

    response = {
        "Wskaźnik": sensor_type.name,
        "Opis": sensor_type.value,
        "Miasto": city,
        "Średnia": mean_value
    }

    return response


def handle_data(data):
    mean = 0
    cnt = 0
    for sample in data:
        if sample["Wartość"] is not None:
            mean += sample["Wartość"]
            cnt += 1
    
    if cnt == 0:
        return "BRAK DANYCH"
    return str(round(mean/cnt,2))



@app.get("/get-all-values")
async def get_all_mean_value(city: str):

    try:
        stations_ids = await find_city_stations_ids(city)
    except HTTPException as e:
        if e.status_code == 404:
            raise HTTPException(status_code=404, detail="Brak danych o danym mieście")

    all_mean_values = []

    for sensor_type in SensorType:
        sensor_mean_value = await get_mean_value(city, sensor_type)
        all_mean_values.append(sensor_mean_value)

    weather_data = await get_weather_data(city)
    all_mean_values.append(weather_data)
    
    return all_mean_values

    
@app.get("/weather")
async def get_weather_data(city: str):

    city_name = handle_polish_names(city)

    url = f"https://danepubliczne.imgw.pl/api/data/synop/station/{city_name}"

    response = requests.get(url)

    if response.status_code != 200:
        raise HTTPException(status_code=500, detail=f"Couldn't get weather data:")
    

    return response.json()
    

def handle_polish_names(city_name: str) -> str:
    polish_to_uni_letters = {
        "ą": "a",
        "ę": "e",
        "ć": "c",
        "ł": "l",
        "ó": "o",
        "ś": "s",
        "ź": "z",
        "ż": "ź"
    }

    city_name = city_name.lower()
    uni_city_name = ""

    for i in range(len(city_name)):
        letter = city_name[i]
        if letter in polish_to_uni_letters.keys():
            uni_city_name += polish_to_uni_letters.get(letter)
        else:
            uni_city_name += letter

    return uni_city_name


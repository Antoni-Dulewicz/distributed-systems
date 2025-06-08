from light import get_light_info,turn_on,turn_off,list_light_devices,set_color, set_brightness
from fridge import list_fridge_devices,set_fridge_temp,get_fridge_info
from fastapi.middleware.cors import CORSMiddleware
from temp import list_temp_devices,get_device_temp
from fastapi.templating import Jinja2Templates
from fastapi.staticfiles import StaticFiles
from fastapi.responses import HTMLResponse
from fastapi import FastAPI, Request, HTTPException
from pydantic import BaseModel
import uvicorn
import grpc

app = FastAPI()

app.mount("/static", StaticFiles(directory="static"), name="static")
templates = Jinja2Templates(directory="static")


app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/", response_class=HTMLResponse)
async def index(request: Request):
    return templates.TemplateResponse("index.html", {
        "request": request
    })


@app.get("/devices")
async def index(request: Request):
    with grpc.insecure_channel('127.0.0.1:50051') as channel:
        fridges = list_fridge_devices(channel)
        lights = list_light_devices(channel)
        thermometers = list_temp_devices(channel)
        
    return fridges + lights + thermometers



class TempRequest(BaseModel):
    fridge_id: str
    temperature: float
    freezer_temperature: float | None = None

@app.post("/set-fridge-temp")
async def set_fridge_temp_request(request: TempRequest):
    try:
        with grpc.insecure_channel('127.0.0.1:50051') as channel:
            response = set_fridge_temp(channel, request.fridge_id,request.temperature, request.freezer_temperature)
        return {"success": response.success, "message": response.message}
    
    except grpc.RpcError as e:
        details = e.details() if e.details() else "Unknown gRPC error"
        raise HTTPException(status_code=400, detail=f"gRPC error: {details}")



class LightSwitchRequest(BaseModel):
    light_id: str
    
@app.post("/switch-light")
async def switch_light(request: LightSwitchRequest):
    try:
        with grpc.insecure_channel('127.0.0.1:50051') as channel:
            is_on = get_light_info(channel, request.light_id).is_on
            if is_on:
                response = turn_off(channel,request.light_id)
            else:
                response = turn_on(channel,request.light_id)

        return {"success": response.success, "message": response.message}

    except grpc.RpcError as e:
        details = e.details() if e.details() else "Unknown gRPC error"
        raise HTTPException(status_code=400, detail=f"gRPC error: {details}")



class ColorChangeRequest(BaseModel):
    light_id: str
    color: str

@app.post("/change-color")
async def color_change(request: ColorChangeRequest):
    try:
        with grpc.insecure_channel('127.0.0.1:50051') as channel:
            response = set_color(channel, request.light_id, request.color)
        return {"success": response.success, "message": response.message}
    except grpc.RpcError as e:
        details = e.details() if e.details() else "Unknown gRPC error"
        raise HTTPException(status_code=400, detail=f"gRPC error: {details}")




class BrightnessChangeRequest(BaseModel):
    light_id: str
    brightness: float
    
@app.post("/change-brightness")
async def brightness_change(request: BrightnessChangeRequest):
    try:
        with grpc.insecure_channel('127.0.0.1:50051') as channel:
            response = set_brightness(channel, request.light_id, request.brightness)
        return {"success": response.success, "message": response.message}
    except grpc.RpcError as e:
        details = e.details() if e.details() else "Unknown gRPC error"
        raise HTTPException(status_code=400, detail=f"gRPC error: {details}")




if __name__ == "__main__":
    uvicorn.run(app, host="127.0.0.1", port=8000)
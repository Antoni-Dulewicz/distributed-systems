from google.protobuf import empty_pb2
import temperature_pb2_grpc,temperature_pb2
import common_pb2
import json

def get_device_temp(channel, device_id):
    stub = temperature_pb2_grpc.TemperatureStub(channel)
    request = common_pb2.DeviceRequest(id=device_id)

    response = stub.GetTemperature(request)
    print("-----------------------------")
    print("Temperature: ", response.temperature)
    print("-----------------------------")



def list_temp_devices(channel):
    stub = temperature_pb2_grpc.TemperatureStub(channel)
    response = stub.ListDevices(empty_pb2.Empty())  

    all_temps = []
    print("-----------------------------")
    for device in response.devices:
        temp_data = {
            "id": device.id,
            "d_type": common_pb2.DeviceType.Name(device.d_type),
            "temperature": device.temperature
        }

        all_temps.append(temp_data)

        print(json.dumps(temp_data, indent=4))
    print("-----------------------------")
    return all_temps



from google.protobuf import empty_pb2
import light_pb2_grpc, light_pb2
import common_pb2
import grpc
import json


def turn_on(channel, light_id):
    try:
        stub = light_pb2_grpc.LightStub(channel)
        request = light_pb2.TurnOnRequest(id=light_id)

        response = stub.TurnOn(request)
        print("-----------------------------")
        print("Success:", response.success)
        print("Message:", response.message)
        print("-----------------------------")
    except grpc.RpcError as e:
        raise e

    return response


def turn_off(channel, light_id):
    try:
        stub = light_pb2_grpc.LightStub(channel)
        request = light_pb2.TurnOffRequest(id=light_id)

        response = stub.TurnOff(request)
        print("-----------------------------")
        print("Success:", response.success)
        print("Message:", response.message)
        print("-----------------------------")
        
    except grpc.RpcError as e:
        raise e    

    return response

def set_color(channel, light_id, color):
    try:
        stub = light_pb2_grpc.LightStub(channel)
        request = light_pb2.SetColorRequest(id=light_id, color=color)

        response = stub.SetColor(request)
        print("-----------------------------")
        print("Success:", response.success)
        print("Message:", response.message)
        print("-----------------------------")

    except grpc.RpcError as e:
        raise e

    return response


def set_brightness(channel, light_id, brightness):
    try:
        stub = light_pb2_grpc.LightStub(channel)
        request = light_pb2.SetBrightnessRequest(id=light_id, brightness=brightness)

        response = stub.SetBrightness(request)
        print("-----------------------------")
        print("Success:", response.success)
        print("Message:", response.message)
        print("-----------------------------")

    except grpc.RpcError as e:
        raise e

    return response

def get_light_info(channel, light_id):
    stub = light_pb2_grpc.LightStub(channel)

    try:

        request = common_pb2.DeviceRequest(id=light_id)
        response = stub.GetLightInfo(request)

        print("-----------------------------")
        print("Is on:", response.is_on)
        print("Type:", light_pb2.LightType.Name(response.type))

        if response.HasField("color"):
            print("Color:", response.color)
        if response.HasField("brightness"):
            print("Brightness:", response.brightness)
        print("-----------------------------")

    except grpc.RpcError as e:
        raise e
    
    return response
        

def list_light_devices(channel):
    stub = light_pb2_grpc.LightStub(channel)
    response = stub.ListLights(empty_pb2.Empty())

    all_lights = []
    print("-----------------------------")
    for device in response.devices:
        
        light_data = {
            "id": device.id,
            "d_type": common_pb2.DeviceType.Name(device.d_type),
            "is_on": device.is_on,
            "light_type": light_pb2.LightType.Name(device.type)
        }

        if device.type == light_pb2.LightType.RGB:
            light_data["color"] = device.color
        else:
            light_data["color"] = None
        
        if device.type == light_pb2.LightType.DIMMABLE:
            light_data["brightness"] = device.brightness
        else:
            light_data["brightness"] = None

        all_lights.append(light_data)

        print(json.dumps(light_data, indent=4))
        
    
    print("-----------------------------")
    return all_lights


from google.protobuf import empty_pb2
import fridge_pb2_grpc,fridge_pb2
import common_pb2
import grpc
import json

def get_fridge_info(channel, fridge_id):
    stub = fridge_pb2_grpc.FridgeStub(channel)

    try:
        request = common_pb2.DeviceRequest(id=fridge_id)
        response = stub.GetFridgeInfo(request)

        print("Temperature:", response.temperature)
        print("Door Closed:", response.door_closed)
        print("Type:", fridge_pb2.FridgeType.Name(response.type))
        if response.HasField("freezer_temperature"):
            print("Freezer Temp:", response.freezer_temperature)

    except grpc.RpcError as e:
        raise e

    


def set_fridge_temp(channel,fridge_id, temp, freezer_temp=None):
    stub = fridge_pb2_grpc.FridgeStub(channel)
    if freezer_temp:
        request = fridge_pb2.SetTemperatureRequest(id=fridge_id, temperature=temp, freezer_temperature=freezer_temp)
    else:
        request = fridge_pb2.SetTemperatureRequest(id=fridge_id, temperature=temp)

    response = stub.SetTemperature(request)
    print("Success:", response.success)
    print("Message:", response.message)
    return response

def list_fridge_devices(channel):
    stub = fridge_pb2_grpc.FridgeStub(channel)
    response = stub.ListFridges(empty_pb2.Empty())
    all_fridges = []

    print("-----------------------------")
    for device in response.devices:
        fridge_data = {
            "id": device.id,
            "d_type": common_pb2.DeviceType.Name(device.d_type),
            "temperature": device.temperature,
            "door_closed": device.door_closed,
            "fridge_type": fridge_pb2.FridgeType.Name(device.type),
        }

        if device.type == fridge_pb2.FridgeType.WITH_FREEZER:
            fridge_data["freezer_temperature"] = device.freezer_temperature
        else:
            fridge_data["freezer_temperature"] = None
        
        all_fridges.append(fridge_data)

        print(json.dumps(fridge_data, indent=4))

    print("-----------------------------")
    return all_fridges
    
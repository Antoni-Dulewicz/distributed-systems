package com.smarthome.devices;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import proto.Common;
import proto.FridgeGrpc;
import proto.FridgeOuterClass.*;

import java.util.HashMap;
import java.util.Map;

public class FridgeServiceImpl extends FridgeGrpc.FridgeImplBase {

    private final Map<String, FridgeInfo> fridgeInfoMap = new HashMap<>();

    public FridgeServiceImpl() {

        FridgeInfo fridge1 = FridgeInfo.newBuilder()
                .setId("fridge1")
                .setDType(Common.DeviceType.REFRIGERATOR)
                .setTemperature(4.0)
                .setDoorClosed(true)
                .setType(FridgeType.WITH_FREEZER)
                .setFreezerTemperature(-5.0)
                .build();

        FridgeInfo fridge2 = FridgeInfo.newBuilder()
                .setId("fridge2")
                .setDType(Common.DeviceType.REFRIGERATOR)
                .setTemperature(5.5)
                .setDoorClosed(false)
                .setType(FridgeType.NO_FREEZER)
                .build();

        FridgeInfo fridge3 = FridgeInfo.newBuilder()
                .setId("fridge3")
                .setDType(Common.DeviceType.REFRIGERATOR)
                .setTemperature(3.2)
                .setDoorClosed(true)
                .setType(FridgeType.WITH_FREEZER)
                .setFreezerTemperature(-3.0)
                .build();

        fridgeInfoMap.put(fridge1.getId(), fridge1);
        fridgeInfoMap.put(fridge2.getId(), fridge2);
//        fridgeInfoMap.put(fridge3.getId(), fridge3);
    }

    @Override
    public void listFridges(Empty request, StreamObserver<FridgeList> responseObserver){
        FridgeList.Builder fridgeListBuilder = FridgeList.newBuilder();

        for(Map.Entry<String, FridgeInfo> set : fridgeInfoMap.entrySet()){
            fridgeListBuilder.addDevices(set.getValue());
        }

        responseObserver.onNext(fridgeListBuilder.build());
        responseObserver.onCompleted();

    }

    @Override
    public void setTemperature(SetTemperatureRequest request, StreamObserver<SetTemperatureResponse> responseObserver){
        String fridgeId = request.getId();
        double temperature = request.getTemperature();
        double freezer_temperature = request.getFreezerTemperature();

        if(!fridgeInfoMap.containsKey(fridgeId)){
            responseObserver.onError(
                    io.grpc.Status.NOT_FOUND
                            .withDescription("Fridge with ID '" + fridgeId + "' not found.")
                            .asRuntimeException()
            );
        }
        FridgeInfo oldInfo = fridgeInfoMap.get(fridgeId);

        FridgeInfo.Builder infoBuilder = FridgeInfo.newBuilder(oldInfo);
        infoBuilder.setTemperature(temperature);
        if(oldInfo.hasFreezerTemperature()){
            infoBuilder.setFreezerTemperature(freezer_temperature);
        }
        FridgeInfo updatedInfo = infoBuilder.build();
        fridgeInfoMap.put(fridgeId, updatedInfo);

        SetTemperatureResponse response = SetTemperatureResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Temperatura poprawnie zmieniona.")
                .build();


        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    @Override
    public void getFridgeInfo(Common.DeviceRequest request, StreamObserver<FridgeInfo> responseObserver){
        String fridgeId = request.getId();

        if(!fridgeInfoMap.containsKey(fridgeId)){
            responseObserver.onError(
                    io.grpc.Status.NOT_FOUND
                            .withDescription("Fridge with ID '" + fridgeId + "' not found.")
                            .asRuntimeException()
            );
        }
        FridgeInfo info = fridgeInfoMap.get(fridgeId);

        responseObserver.onNext(info);
        responseObserver.onCompleted();


    }
}

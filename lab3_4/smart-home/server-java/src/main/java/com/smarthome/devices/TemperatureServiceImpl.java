package com.smarthome.devices;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;

import proto.Common;
import proto.TemperatureGrpc;
import proto.TemperatureOuterClass.*;
import proto.Common.DeviceRequest;

import java.util.HashMap;
import java.util.Map;

public class TemperatureServiceImpl extends TemperatureGrpc.TemperatureImplBase {

    private final Map<String, TempInfo > tempInfoMap = new HashMap<>();


    public TemperatureServiceImpl() {
        TempInfo thermometer1 = TempInfo.newBuilder()
                .setId("thermometer1")
                .setDType(Common.DeviceType.THERMOMETER)
                .setTemperature(22.5)
                .build();

        TempInfo thermometer2 = TempInfo.newBuilder()
                .setId("thermometer2")
                .setDType(Common.DeviceType.THERMOMETER)
                .setTemperature(25.3)
                .build();

        TempInfo thermometer3 = TempInfo.newBuilder()
                .setId("thermometer3")
                .setDType(Common.DeviceType.THERMOMETER)
                .setTemperature(19.8)
                .build();

        TempInfo thermometer4 = TempInfo.newBuilder()
                .setId("thermometer4")
                .setDType(Common.DeviceType.THERMOMETER)
                .setTemperature(15.8)
                .build();

        tempInfoMap.put(thermometer1.getId(),thermometer1);
        tempInfoMap.put(thermometer2.getId(),thermometer2);
        tempInfoMap.put(thermometer3.getId(),thermometer3);
        tempInfoMap.put(thermometer4.getId(),thermometer4);

    }

    @Override
    public void listDevices(Empty request, StreamObserver<TempList> responseObserver) {
        TempList.Builder tempListBuilder = TempList.newBuilder();

        for (Map.Entry<String, TempInfo> set : tempInfoMap.entrySet()) {
            tempListBuilder.addDevices(set.getValue());
        }

        responseObserver.onNext(tempListBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getTemperature(DeviceRequest request, StreamObserver<TempInfo> responseObserver) {
        String deviceId = request.getId();

        if(!tempInfoMap.containsKey(deviceId)){
            responseObserver.onError(
                    io.grpc.Status.NOT_FOUND
                            .withDescription("Light with ID '" + deviceId + "' not found.")
                            .asRuntimeException()
            );
            return;
        }

        responseObserver.onNext(tempInfoMap.get(deviceId));
        responseObserver.onCompleted();

    }
}

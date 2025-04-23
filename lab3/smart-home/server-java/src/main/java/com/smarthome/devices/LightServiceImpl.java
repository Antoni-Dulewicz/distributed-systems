package com.smarthome.devices;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import proto.Common;
import proto.FridgeOuterClass;
import proto.LightGrpc;
import proto.LightOuterClass.*;

import java.util.HashMap;
import java.util.Map;

public class LightServiceImpl extends LightGrpc.LightImplBase {

    private final Map<String, LightInfo> lightInfoMap = new HashMap<>();


    public LightServiceImpl() {

        LightInfo light1 = LightInfo.newBuilder()
                .setId("light1")
                .setDType(Common.DeviceType.LIGHT)
                .setIsOn(true)
                .setType(LightType.NORMAL)
                .build();

        LightInfo light2 = LightInfo.newBuilder()
                .setId("light2")
                .setDType(Common.DeviceType.LIGHT)
                .setIsOn(false)
                .setType(LightType.RGB)
                .setColor("red")
                .build();

        LightInfo light3 = LightInfo.newBuilder()
                .setId("light3")
                .setDType(Common.DeviceType.LIGHT)
                .setIsOn(false)
                .setType(LightType.DIMMABLE)
                .setBrightness(50)
                .build();

        lightInfoMap.put(light1.getId(), light1);
        lightInfoMap.put(light2.getId(), light2);
        lightInfoMap.put(light3.getId(), light3);
    }

    @Override
    public void turnOn(TurnOnRequest request, StreamObserver<LightResponse> responseObserver){
        String lightId = request.getId();

        if(!lightInfoMap.containsKey(lightId)){
            responseObserver.onError(
                    io.grpc.Status.NOT_FOUND
                            .withDescription("Light with ID '" + lightId + "' not found.")
                            .asRuntimeException()
            );
        }
        LightInfo info = lightInfoMap.get(lightId);

        LightInfo updatedInfo = LightInfo.newBuilder(info)
                .setIsOn(true)
                .build();

        lightInfoMap.put(updatedInfo.getId(), updatedInfo);

        LightResponse response = LightResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Light turned on ")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();


    }

    @Override
    public void turnOff(TurnOffRequest request, StreamObserver<LightResponse> responseObserver){
        String lightId = request.getId();

        if(!lightInfoMap.containsKey(lightId)){
            responseObserver.onError(
                    io.grpc.Status.NOT_FOUND
                            .withDescription("Light with ID '" + lightId + "' not found.")
                            .asRuntimeException()
            );
            return;
        }

        LightInfo info = lightInfoMap.get(lightId);

        LightInfo updatedInfo = LightInfo.newBuilder(info)
                .setIsOn(false)
                .build();

        lightInfoMap.put(updatedInfo.getId(), updatedInfo);

        LightResponse response = LightResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Light turned off ")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    @Override
    public void setColor(SetColorRequest request, StreamObserver<LightResponse> responseObserver){
        String lightId = request.getId();

        if(!lightInfoMap.containsKey(lightId)){
            responseObserver.onError(
                    io.grpc.Status.NOT_FOUND
                            .withDescription("Light with ID '" + lightId + "' not found.")
                            .asRuntimeException()
            );
            return;
        }

        LightInfo info = lightInfoMap.get(lightId);

        if(!info.hasColor()){
            responseObserver.onError(
                    Status.FAILED_PRECONDITION
                            .withDescription("Light with ID '" + lightId + "' doesn't have color option.")
                            .asRuntimeException()
            );
            return;
        }

        LightInfo updatedInfo = LightInfo.newBuilder(info)
                .setColor(request.getColor())
                .build();

        lightInfoMap.put(updatedInfo.getId(), updatedInfo);

        LightResponse response = LightResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Light color changed")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    @Override
    public void setBrightness(SetBrightnessRequest request, StreamObserver<LightResponse> responseObserver){
        String lightId = request.getId();

        if(!lightInfoMap.containsKey(lightId)){
            responseObserver.onError(
                    io.grpc.Status.NOT_FOUND
                            .withDescription("Light with ID '" + lightId + "' not found.")
                            .asRuntimeException()
            );
            return;
        }


        LightInfo info = lightInfoMap.get(lightId);

        if(!info.hasBrightness()){
            responseObserver.onError(
                    Status.FAILED_PRECONDITION
                            .withDescription("Light with ID '" + lightId + "' doesn't have brightness option.")
                            .asRuntimeException()
            );
            return;
        }

        if(request.getBrightness() < 0 || request.getBrightness() > 100){
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Wrong brightness value, can be set between 0 and 100.")
                            .asRuntimeException()
            );
            return;
        }

        LightInfo updatedInfo = LightInfo.newBuilder(info)
                .setBrightness(request.getBrightness())
                .build();

        lightInfoMap.put(updatedInfo.getId(), updatedInfo);

        LightResponse response = LightResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Light brightness changed")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    @Override
    public void getLightInfo(Common.DeviceRequest request, StreamObserver<LightInfo> responseObserver){
        String lightId = request.getId();

        if(!lightInfoMap.containsKey(lightId)){
            responseObserver.onError(
                    io.grpc.Status.NOT_FOUND
                            .withDescription("Fridge with ID '" + lightId + "' not found.")
                            .asRuntimeException()
            );
        }

        LightInfo info = lightInfoMap.get(lightId);

        responseObserver.onNext(info);
        responseObserver.onCompleted();

    }

    @Override
    public void listLights(Empty request, StreamObserver<LightList> responseObserver){
        LightList.Builder lightListBuilder = LightList.newBuilder();

        for(Map.Entry<String, LightInfo> set : lightInfoMap.entrySet()){
            lightListBuilder.addDevices(set.getValue());
        }

        responseObserver.onNext(lightListBuilder.build());
        responseObserver.onCompleted();
    }

}

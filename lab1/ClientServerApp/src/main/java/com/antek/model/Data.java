package com.antek.model;

import java.io.Serializable;

public record Data(Requests requestType, String message) implements Serializable {
}

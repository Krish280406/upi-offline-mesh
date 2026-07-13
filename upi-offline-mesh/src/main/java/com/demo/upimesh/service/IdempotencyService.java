package com.demo.upimesh.service;

public interface IdempotencyService {
    boolean claim(String packetHash);
    int size();
    void clear();
}
package com.donphds.crypto.excpetion;

public class CryptoException extends RuntimeException {
    public CryptoException(String msg) {
        super(msg);
    }

    public CryptoException(Throwable e) {
        super(e);
    }
}

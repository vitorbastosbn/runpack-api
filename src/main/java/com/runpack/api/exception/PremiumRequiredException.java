package com.runpack.api.exception;

/**
 * 403 com código de máquina (ex: GROUP_LIMIT_REACHED) — o mobile usa o código
 * para abrir a Paywall em vez de mostrar erro genérico.
 */
public class PremiumRequiredException extends RuntimeException {

    private final String code;

    public PremiumRequiredException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

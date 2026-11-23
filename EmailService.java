package com.academicrace.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    // Por enquanto o código não é enviado por e-mail: aparece só no log do backend
    public void enviarCodigoRedefinicao(String destino, String codigo) {
        log.warn("E-mail não configurado. Código de redefinição para {}: {}", destino, codigo);
    }
}
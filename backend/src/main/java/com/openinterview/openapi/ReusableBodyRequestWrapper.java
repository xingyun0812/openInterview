package com.openinterview.openapi;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class ReusableBodyRequestWrapper extends HttpServletRequestWrapper {
    private final byte[] body;

    public ReusableBodyRequestWrapper(HttpServletRequest request, byte[] body) {
        super(request);
        this.body = body == null ? new byte[0] : body;
    }

    public byte[] getBody() {
        return body;
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream bais = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override
            public int read() {
                return bais.read();
            }

            @Override
            public boolean isFinished() {
                return bais.available() <= 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // sync
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        Charset cs = Charset.forName(getCharacterEncoding() == null ? "UTF-8" : getCharacterEncoding());
        return new BufferedReader(new InputStreamReader(getInputStream(), cs));
    }
}


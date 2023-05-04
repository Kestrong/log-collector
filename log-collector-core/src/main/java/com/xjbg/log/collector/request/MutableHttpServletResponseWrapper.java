package com.xjbg.log.collector.request;

import javax.annotation.Nonnull;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.Predicate;

/**
 * @author kesc
 * @since 2023-04-20 15:39
 */
public class MutableHttpServletResponseWrapper extends HttpServletResponseWrapper {
    private final ByteArrayOutputStream buffer;
    private ServletOutputStream out;
    private final Predicate<String> canConsume;

    public MutableHttpServletResponseWrapper(HttpServletResponse httpServletResponse, Predicate<String> canConsume) {
        super(httpServletResponse);
        this.canConsume = canConsume;
        buffer = new ByteArrayOutputStream(1024);
    }

    private boolean canConsume() {
        return canConsume != null && canConsume.test(getResponse().getContentType());
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (!canConsume()) {
            return super.getOutputStream();
        }
        if (out == null) {
            out = new WrapperOutputStream(getResponse().getOutputStream());
        }
        return out;
    }

    public byte[] getContent() throws IOException {
        flushBuffer();
        return buffer.size() > 0 ? buffer.toByteArray() : null;
    }

    class WrapperOutputStream extends ServletOutputStream {
        private final ServletOutputStream os;

        public WrapperOutputStream(ServletOutputStream os) {
            this.os = os;
        }

        @Override
        public void write(int b) throws IOException {
            buffer.write(b);
            os.write(b);
        }

        @Override
        public void write(@Nonnull byte[] b, int off, int len) throws IOException {
            buffer.write(b, off, len);
            os.write(b, off, len);
        }

        @Override
        public boolean isReady() {
            return this.os.isReady();
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            this.os.setWriteListener(writeListener);
        }
    }

}

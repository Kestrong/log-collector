package com.xjbg.log.collector.request;

import lombok.Getter;

import javax.annotation.Nonnull;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.Predicate;

/**
 * @author kesc
 * @since 2023-04-14 14:41
 */
@Getter
public class MutableHttpServletRequestWrapper extends HttpServletRequestWrapper {
    private final Map<String, String> customHeaders;
    private final ByteArrayOutputStream cachedContent;
    private ServletInputStream is;
    private BufferedReader reader;
    private final Predicate<String> canConsume;

    public MutableHttpServletRequestWrapper(HttpServletRequest request, Predicate<String> canConsume) throws IOException {
        super(request);
        this.customHeaders = new HashMap<>();
        this.canConsume = canConsume;
        int contentLength = request.getContentLength();
        this.cachedContent = canConsume() ? new ByteArrayOutputStream(contentLength >= 0 ? contentLength : 1024) : new ByteArrayOutputStream(0);
    }

    public void addHeader(String name, String value) {
        this.customHeaders.put(name, value);
    }

    @Override
    public String getHeader(String name) {
        String headerValue = customHeaders.get(name);

        if (headerValue != null) {
            return headerValue;
        }
        return ((HttpServletRequest) getRequest()).getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        if (customHeaders.isEmpty()) {
            return super.getHeaderNames();
        }

        Set<String> set = new HashSet<>(customHeaders.keySet());
        Enumeration<String> e = ((HttpServletRequest) getRequest()).getHeaderNames();
        while (e.hasMoreElements()) {
            String n = e.nextElement();
            set.add(n);
        }

        return Collections.enumeration(set);
    }

    private boolean canConsume() {
        return canConsume != null && canConsume.test(getRequest().getContentType());
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (!canConsume()) {
            return getRequest().getInputStream();
        }
        if (is == null) {
            is = new CachedServletInputStream(getRequest().getInputStream());
        }
        return is;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (!canConsume()) {
            return super.getReader();
        }
        if (reader == null) {
            reader = new BufferedReader(new InputStreamReader(this.getInputStream()));
        }
        return reader;
    }

    public byte[] getRequestBody() {
        return cachedContent.size() > 0 ? cachedContent.toByteArray() : null;
    }

    class CachedServletInputStream extends ServletInputStream {
        private final ServletInputStream is;

        public CachedServletInputStream(ServletInputStream is) {
            this.is = is;
        }

        @Override
        public boolean isFinished() {
            return is.isFinished();
        }

        @Override
        public boolean isReady() {
            return is.isReady();
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            is.setReadListener(readListener);
        }

        @Override
        public int read() throws IOException {
            int ch = this.is.read();
            if (ch != -1) {
                cachedContent.write(ch);
            }
            return ch;
        }

        @Override
        public int read(@Nonnull byte[] b) throws IOException {
            int count = this.is.read(b);
            writeToCache(b, 0, count);
            return count;
        }

        private void writeToCache(final byte[] b, final int off, int count) {
            if (count > 0) {
                cachedContent.write(b, off, count);
            }
        }

        @Override
        public int read(@Nonnull final byte[] b, final int off, final int len) throws IOException {
            int count = this.is.read(b, off, len);
            writeToCache(b, off, count);
            return count;
        }

        @Override
        public int readLine(final byte[] b, final int off, final int len) throws IOException {
            int count = this.is.readLine(b, off, len);
            writeToCache(b, off, count);
            return count;
        }
    }
}

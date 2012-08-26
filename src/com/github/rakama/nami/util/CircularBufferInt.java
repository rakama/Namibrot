/*
 * Copyright (c) 2012, RamsesA <ramsesakama@gmail.com>
 * 
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
 * INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM
 * LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR
 * OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THIS SOFTWARE.
 */

package com.github.rakama.nami.util;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

public final class CircularBufferInt
{
    private final int[] buffer;
    private int start, end, size;

    public CircularBufferInt(int capacity)
    {
        buffer = new int[capacity];
    }

    public void clear()
    {
        size = start = end = 0;
    }
    
    public void push(int val)
    {
        if(size == buffer.length)
            throw new BufferOverflowException();

        buffer[end] = val;
        end++;

        if(end >= buffer.length)
            end = 0;

        size++;
    }

    public int poll()
    {
        if(start == end)
            throw new BufferUnderflowException();

        size--;

        int val = buffer[start];
        start++;

        if(start >= buffer.length)
            start = 0;

        return val;
    }
    
    protected void copy(CircularBufferInt src)
    {
        if(src.capacity() != capacity())
            throw new IllegalArgumentException();
        
        System.arraycopy(src.buffer, 0, this.buffer, 0, src.buffer.length);
        start = src.start;
        end = src.end;
        size = src.size;
    }

    public int size()
    {
        return size;
    }

    public int capacity()
    {
        return buffer.length;
    }
    
    public boolean isFull()
    {
        return size == buffer.length;
    }
    
    public boolean isEmpty()
    {
        return start == end;
    }
}
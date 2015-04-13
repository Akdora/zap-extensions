/*
 * Zed Attack Proxy (ZAP) and its related class files.
 * 
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 * 
 * Copyright 2015 The ZAP Development Team
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zaproxy.zap.extension.fuzz.payloads.generator;

import org.zaproxy.zap.extension.fuzz.payloads.Payload;
import org.zaproxy.zap.utils.ResettableAutoCloseableIterator;

public class EmptyPayloadGenerator<T1, T2 extends Payload<T1>> implements PayloadGenerator<T1, T2> {

    private final T2 value;
    private final int numberOfPayloads;

    public EmptyPayloadGenerator(T2 value, int numberOfPayloads) {
        if (value == null) {
            throw new IllegalArgumentException("Parameter value must not be null.");
        }
        if (numberOfPayloads <= 0) {
            throw new IllegalArgumentException("Parameter numberOfPayloads must be greater than zero.");
        }

        this.value = value;
        this.numberOfPayloads = numberOfPayloads;
    }

    @Override
    public long getNumberOfPayloads() {
        return numberOfPayloads;
    }

    @Override
    public ResettableAutoCloseableIterator<T2> iterator() {
        return new ValueRepeaterIterator<>(value, numberOfPayloads);
    }

    @Override
    public PayloadGenerator<T1, T2> copy() {
        return new EmptyPayloadGenerator<>(value, numberOfPayloads);
    }

    private static class ValueRepeaterIterator<T, E extends Payload<T>> implements ResettableAutoCloseableIterator<E> {

        private final E value;
        private final int repeat;
        private int current;

        public ValueRepeaterIterator(E value, int repeat) {
            this.repeat = repeat;
            this.value = value;
        }

        @Override
        public boolean hasNext() {
            return current < repeat;
        }

        @Override
        public E next() {
            current++;
            return value;
        }

        @Override
        public void remove() {
        }

        @Override
        public void reset() {
            current = 0;
        }

        @Override
        public void close() throws Exception {
        }
    }
}

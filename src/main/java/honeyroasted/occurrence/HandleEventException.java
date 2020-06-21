/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 socraticphoenix@gmail.com
 * Copyright (c) 2016 contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package honeyroasted.occurrence;

import java.util.ArrayList;
import java.util.List;

public class HandleEventException extends RuntimeException {
    private List<Entry> errors;

    public HandleEventException(Object event, ListenerWrapper wrapper, Throwable cause) {
        this(event, new ArrayList<>());
        this.errors.add(new Entry(cause, wrapper));
    }

    public HandleEventException(Object event, List<Entry> errors) {
        super(genMessage(event, errors));
        this.errors = errors;
        errors.forEach(e -> addSuppressed(e.exception));
    }

    private static String genMessage(Object event, List<Entry> errors) {
        StringBuilder message = new StringBuilder();
        message.append("Failed to pass event ").append(event == null ? "null" : event.getClass()).append(" to listeners [");
        for (int i = 0; i < errors.size(); i++) {
            message.append(errors.get(i).wrapper.name());
            if (i < errors.size() - 1){
                message.append(", ");
            }
        }
        return message.append("]").toString();
    }

    public List<Entry> getErrors() {
        return errors;
    }

    public static class Entry {
        private Throwable exception;
        private ListenerWrapper wrapper;

        public Entry(Throwable exception, ListenerWrapper wrapper) {
            this.exception = exception;
            this.wrapper = wrapper;
        }

        public Throwable getException() {
            return exception;
        }

        public ListenerWrapper getWrapper() {
            return wrapper;
        }

    }

}

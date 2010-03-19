package org.jruby.ext;

import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyKernel;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;
import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;

/**
 * This library adds reference queue support to JRuby's weakrefs by adding a
 * RefQueue class that wraps a Java ReferenceQueue and replacing the built-in
 * WeakRef impl with a new one that's aware of RefQueue.
 * 
 * @author headius
 */
public class RefQueueLibrary implements Library {
    public void load(Ruby runtime, boolean wrap) throws IOException {
        RubyKernel.require(runtime.getKernel(), runtime.newString("weakref"), Block.NULL_BLOCK);

        RubyClass weakrefClass = (RubyClass)runtime.getClassFromPath("WeakRef");
        weakrefClass.setAllocator(WEAKREF_ALLOCATOR);
        weakrefClass.defineAnnotatedMethods(WeakRef.class);

        RubyClass refQueueClass = runtime.defineClassUnder("RefQueue", runtime.getObject(), REFQUEUE_ALLOCATOR, weakrefClass);
        refQueueClass.defineAnnotatedMethods(RefQueue.class);
    }
    
    private static final ObjectAllocator WEAKREF_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
            return new WeakRef(runtime, klazz);
        }
    };

    private static final ObjectAllocator REFQUEUE_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
            return new RefQueue(runtime, klazz);
        }
    };

    @JRubyClass(name="RefQueue", parent="Object")
    public static class RefQueue extends RubyObject {
        private final ReferenceQueue queue;

        public RefQueue(Ruby runtime, RubyClass klass) {
            super(runtime, klass);
            queue = new ReferenceQueue();
        }

        public ReferenceQueue getQueue() {
            return queue;
        }

        @JRubyMethod
        public IRubyObject poll() {
            return returnable(queue.poll());
        }

        @JRubyMethod
        public IRubyObject remove() {
            try {
                return returnable(queue.remove());
            } catch (InterruptedException ie) {
                // ignore
                return getRuntime().getNil();
            }
        }

        @JRubyMethod
        public IRubyObject remove(IRubyObject timeout) {
            try {
                return returnable(queue.remove(timeout.convertToInteger().getLongValue()));
            } catch (InterruptedException ie) {
                // ignore
                return getRuntime().getNil();
            }
        }

        private IRubyObject returnable(Object result) {
            RubyWeakReference ref = (RubyWeakReference)result;
            if (ref == null) return getRuntime().getNil();
            return ref.getWeakRef();
        }
    }

    public static class RubyWeakReference extends WeakReference<IRubyObject> {
        private final WeakRef ref;
        public RubyWeakReference(IRubyObject obj, WeakRef ref) {
            super(obj);
            this.ref = ref;
        }
        public RubyWeakReference(IRubyObject obj, WeakRef ref, ReferenceQueue queue) {
            super(obj, queue);
            this.ref = ref;
        }
        public WeakRef getWeakRef() {
            return ref;
        }
    }

    public static class WeakRef extends RubyObject {
        private RubyWeakReference ref;

        public WeakRef(Ruby runtime, RubyClass klazz) {
            super(runtime, klazz);
        }

        @JRubyMethod(name = "__getobj__")
        public IRubyObject getobj() {
            IRubyObject obj = ref.get();

            if (obj == null) {
                // FIXME weakref.rb also does caller(2) here for the backtrace
                throw newRefError("Illegal Reference - probably recycled");
            }

            return obj;
        }

        @JRubyMethod(name = "__setobj__")
        public IRubyObject setobj(IRubyObject obj) {
	        return getRuntime().getNil();
	    }

        // This is only here to replace the "new" in JRuby's weakref, which
        // doesn't really need to be there.
        @JRubyMethod(name = "new", required = 1, optional = 1, meta = true)
        public static IRubyObject newInstance(IRubyObject clazz, IRubyObject[] args) {
            WeakRef weakRef = (WeakRef)((RubyClass)clazz).allocate();

            weakRef.callInit(args, Block.NULL_BLOCK);

            return weakRef;
        }

        @JRubyMethod(name = "initialize", frame = true, visibility = Visibility.PRIVATE)
        public IRubyObject initialize(ThreadContext context, IRubyObject obj) {
            ref = new RubyWeakReference(obj, this);

            return RuntimeHelpers.invokeSuper(context, this, obj, Block.NULL_BLOCK);
        }

        @JRubyMethod(name = "initialize", frame = true, visibility = Visibility.PRIVATE)
        public IRubyObject initialize(ThreadContext context, IRubyObject obj, IRubyObject queue) {
            if (!(queue instanceof RefQueue)) {
                throw getRuntime().newTypeError("WeakRef can only queue into a RefQueue");
            }
            ref = new RubyWeakReference(obj, this, ((RefQueue)queue).getQueue());

            return RuntimeHelpers.invokeSuper(context, this, obj, Block.NULL_BLOCK);
        }

        @JRubyMethod(name = "weakref_alive?")
        public IRubyObject weakref_alive_p() {
            return ref.get() != null ? getRuntime().getTrue() : getRuntime().getFalse();
        }

        private RaiseException newRefError(String message) {
            RubyException exception =
                    (RubyException)getRuntime().getClass("RefError").newInstance(getRuntime().getCurrentContext(),
                    new IRubyObject[] {getRuntime().newString(message)}, Block.NULL_BLOCK);

            return new RaiseException(exception);
        }
    }
}

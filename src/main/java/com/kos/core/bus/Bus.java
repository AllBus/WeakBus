package com.kos.core.bus;

import com.kos.core.weakset.WeakHashSet;

/**
 * Created on 16.02.2017.
 *
 * @author Kos
 */
public class Bus {
	public static final String DEFAULT_IDENTIFIER = "default";

	public static final int FLAG_ALL = 0xFFFFFFFF;
	public static final int FLAG_SETTING = 0x04000000;
	public static final int FLAG_DIALOG = 0x02000000;
	public static final int FLAG_ALERT = 0x01000000;

	/**
	 * Identifier used to differentiate the event bus instance.
	 */
	private final String identifier;
	private final BusAction busAction= new BusAction();
	/**
	 * Creates a new Bus named {@code identifier}
	 */
	public Bus(String identifier) {
		this.identifier = identifier;
	}

	/**
	 * All registered event handlers, indexed by event type.
	 */
	final private WeakHashSet<IBusHandler> handlers = new WeakHashSet<>();

	/**
	 * Creates a new Bus named "default"
	 */
	public Bus() {
		this(Bus.DEFAULT_IDENTIFIER);
	}


	@Override
	public String toString() {
		return "[Bus \"" + identifier + "\"]";
	}

	/**
	 * Register object as event handler
	 * @param object  event handler
	 */
	public void register(IBusHandler object) {
		if (object == null) {
			throw new NullPointerException("Object to register must not be null.");
		}
		enforce();
		handlers.put(object.busId(), object);
	}

	/**
	 * Unregister event handler
	 * @param object event handler
	 */
	public void unregister(IBusHandler object) {
		if (object == null) {
			throw new NullPointerException("Object to unregister must not be null.");
		}
		enforce();
		handlers.remove(object.busId());
	}

	/**
	 * Posting object on all handlers with flag contains {@code event} flag
	 * @param event object for handle
	 */
	public void post(IBusSender event) {
		if (event == null) {
			return;
		}
		enforce();

		handlers.forEach(busAction.setup(event));
		handlers.garbage();
	}

	public int handlersCount(){
		return handlers.size();
	}

	public void garbage(){
		handlers.garbage();
	}

	public void handlersClear(){
		handlers.clear();
	}

	/**
	 * override this method for check thread
	 */
	public void enforce() {

	}

	private class BusAction implements WeakHashSet.ISetAction<IBusHandler>{

		int eventFlag;
		IBusSender event;

		public BusAction setup(IBusSender event){
			eventFlag = event.flags();
			this.event=event;
			return this;
		}

		@Override
		public void action(IBusHandler x) {
			if ((eventFlag & x.busFlags()) != 0) {
				x.busUpdate(event);
			}
		}
	}

}

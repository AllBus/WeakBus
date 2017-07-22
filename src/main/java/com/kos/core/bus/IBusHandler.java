package com.kos.core.bus;

/**
 * Interface for all reciver objects
 * Created on 30.01.2017.
 *
 * @author Kos
 */

public interface IBusHandler {
	/**
	 * This method
	 * @param updater message object
	 */
	void busUpdate(IBusSender updater);

	/**
	 * received message types
	 * @return bit flag of received message types
	 */
	int busFlags();

	/**
	 *
	 * @return id of this
	 */
	int busId();
}

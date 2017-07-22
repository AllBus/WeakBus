package com.kos.core.weakset;


import java.lang.ref.WeakReference;

public class WeakHashSet<E> {

	public interface ISetAction<T> {
		void action(T element);
	}

	public void put(E item) {
		put(item.hashCode(), item);
	}

	public void remove(E item) {
		remove(item.hashCode());
	}
		
	@SuppressWarnings("unchecked")
	public void forEach(ISetAction<E> action) {
		int n = mSize;

		Object[] values = mValues;

		for (int i = 0; i < n; i++) {
			Object val = values[i];

			if (val != DELETED) {
				E element = ((WeakReference<E>) val).get();
				if (element == null) {
					values[i] = DELETED;
					mGarbage = true;
				} else {
					action.action(element);
				}
			}
		}
	}

	public void garbage(){
		if (mGarbage)
			gc();
	}

	/**
	 * Remove all items that point to null
	 */
	@SuppressWarnings("unchecked")
	public void optimize() {
		int n = mSize;

		Object[] values = mValues;

		for (int i = 0; i < n; i++) {
			Object val = values[i];

			if (val != DELETED) {
				if (null == ((WeakReference<E>) val).get()) {
					values[i] = DELETED;
					mGarbage = true;
				}
			}
		}

		if (mGarbage)
			gc();
	}


	private static final int[] EMPTY_INTS = new int[0];
	private static final Object[] EMPTY_OBJECTS = new Object[0];

	private static int idealIntArraySize(int need) {
		return idealByteArraySize(need * 4) / 4;
	}

	private static int idealByteArraySize(int need) {
		for (int i = 4; i < 32; i++)
			if (need <= (1 << i) - 12)
				return (1 << i) - 12;

		return need;
	}

	private static int binarySearch(int[] array, int size, int value) {
		int lo = 0;
		int hi = size - 1;

		while (lo <= hi) {
			int mid = (lo + hi) >>> 1;
			int midVal = array[mid];

			if (midVal < value) {
				lo = mid + 1;
			} else if (midVal > value) {
				hi = mid - 1;
			} else {
				return mid;  // value found
			}
		}
		return ~lo;  // value not present
	}

	private static final Object DELETED = null;
	private boolean mGarbage = false;

	private int[] mKeys;
	private Object[] mValues;
	private int mSize;


	public WeakHashSet() {
		this(10);
	}

	public WeakHashSet(int initialCapacity) {
		if (initialCapacity == 0) {
			mKeys = EMPTY_INTS;
			mValues = EMPTY_OBJECTS;
		} else {
			initialCapacity = idealIntArraySize(initialCapacity);
			mKeys = new int[initialCapacity];
			mValues = new Object[initialCapacity];
		}
		mSize = 0;
	}

	//=======================

	/**
	 * Gets the Object mapped from the specified key, or <code>null</code>
	 * if no such mapping has been made.
	 */
	public E get(int key) {
		return get(key, null);
	}

	/**
	 * Gets the Object mapped from the specified key, or the specified Object
	 * if no such mapping has been made.
	 */
	@SuppressWarnings("unchecked")
	public E get(int key, E valueIfKeyNotFound) {
		int i = binarySearch(mKeys, mSize, key);

		if (i < 0 || mValues[i] == DELETED) {
			return valueIfKeyNotFound;
		} else {
			return ((WeakReference<E>) mValues[i]).get();
		}
	}

	/**
	 * Removes the mapping from the specified key, if there was any.
	 */
	private void delete(int key) {
		int i = binarySearch(mKeys, mSize, key);

		if (i >= 0) {
			if (mValues[i] != DELETED) {
				mValues[i] = DELETED;
				mGarbage = true;
			}
		}
	}

	/**
	 * Alias for {@link #delete(int)}.
	 */
	public void remove(int key) {
		delete(key);
	}

	/**
	 * Removes the mapping at the specified index.
	 */
	private void removeAt(int index) {
		if (mValues[index] != DELETED) {
			mValues[index] = DELETED;
			mGarbage = true;
		}
	}

	/**
	 * Remove a range of mappings as a batch.
	 *
	 * @param index Index to begin at
	 * @param size  Number of mappings to remove
	 */
	private void removeAtRange(int index, int size) {
		final int end = Math.min(mSize, index + size);
		for (int i = index; i < end; i++) {
			removeAt(i);
		}
	}

	private void gc() {
		// Log.e("SparseArray", "gc start with " + mSize);

		int n = mSize;
		int o = 0;
		int[] keys = mKeys;
		Object[] values = mValues;

		for (int i = 0; i < n; i++) {
			Object val = values[i];

			if (val != DELETED) {
				if (i != o) {
					keys[o] = keys[i];
					values[o] = val;
					values[i] = null;
				}

				o++;
			}
		}

		mGarbage = false;
		mSize = o;

		// Log.e("SparseArray", "gc end with " + mSize);
	}

	/**
	 * Adds a mapping from the specified key to the specified value,
	 * replacing the previous mapping from the specified key if there
	 * was one.
	 */
	public void put(int key, E item) {
		WeakReference<E> value = new WeakReference<>(item);

		int i = binarySearch(mKeys, mSize, key);

		if (i >= 0) {
			mValues[i] = value;
		} else {
			i = ~i;

			if (i < mSize && mValues[i] == DELETED) {
				mKeys[i] = key;
				mValues[i] = value;
				return;
			}

			if (mGarbage && mSize >= mKeys.length) {
				gc();

				// Search again because indices may have changed.
				i = ~binarySearch(mKeys, mSize, key);
			}

			if (mSize >= mKeys.length) {
				int n = idealIntArraySize(mSize + 1);

				int[] nkeys = new int[n];
				Object[] nvalues = new Object[n];

				// Log.e("SparseArray", "grow " + mKeys.length + " to " + n);
				System.arraycopy(mKeys, 0, nkeys, 0, mKeys.length);
				System.arraycopy(mValues, 0, nvalues, 0, mValues.length);

				mKeys = nkeys;
				mValues = nvalues;
			}

			if (mSize - i != 0) {
				// Log.e("SparseArray", "move " + (mSize - i));
				System.arraycopy(mKeys, i, mKeys, i + 1, mSize - i);
				System.arraycopy(mValues, i, mValues, i + 1, mSize - i);
			}

			mKeys[i] = key;
			mValues[i] = value;
			mSize++;
		}
	}

	/**
	 * Returns the number of key-value mappings that this SparseArray
	 * currently stores.
	 */
	public int size() {
		if (mGarbage) {
			gc();
		}

		return mSize;
	}

	/**
	 * Given an index in the range <code>0...size()-1</code>, returns
	 * the key from the <code>index</code>th key-value mapping that this
	 * SparseArray stores.
	 */
	public int keyAt(int index) {
		if (mGarbage) {
			gc();
		}

		return mKeys[index];
	}

	/**
	 * Given an index in the range <code>0...size()-1</code>, returns
	 * the value from the <code>index</code>th key-value mapping that this
	 * SparseArray stores.
	 */
	@SuppressWarnings("unchecked")
	public E valueAt(int index) {
		if (mGarbage) {
			gc();
		}

		return ((WeakReference<E>) mValues[index]).get();
	}


	/**
	 * Returns the index for which {@link #keyAt} would return the
	 * specified key, or a negative number if the specified
	 * key is not mapped.
	 */
	private int indexOfKey(int key) {
		if (mGarbage) {
			gc();
		}

		return binarySearch(mKeys, mSize, key);
	}

	/**
	 * Removes all key-value mappings from this SparseArray.
	 */
	public void clear() {
		int n = mSize;
		Object[] values = mValues;

		for (int i = 0; i < n; i++) {
			values[i] = null;
		}

		mSize = 0;
		mGarbage = false;
	}

}

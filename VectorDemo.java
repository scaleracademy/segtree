class Vector<T> {
	private T[] ar;
	private int length;
	private int capacity;
	private static final double LOAD_FACTOR = 10;

	public Vector(int capacity, T defaultValue) {
		this.capacity = Math.max(capacity, 1);
		ar = (T[]) new Object[capacity];
		for(int i = 0; i < capacity; i++)
			ar[i] = defaultValue;
		this.length = capacity;
	}

	private void resize(int newCapacity) {
		System.out.println("resizing from " + capacity + " to " + newCapacity);
		// create a new array of larger capacity
		T[] newArray = (T[]) new Object[newCapacity];
		// copy all the previous data into the new array
		for(int i = 0; i < length; i++)
			newArray[i] = ar[i];
		// old array will be replaced by the new array
		ar = newArray;
		capacity = newCapacity;
	}

	public T get(int index) {
		if(index < 0 || index >= length) throw new IndexOutOfBoundsException("index: " + index);
		return ar[index];
	}

	public void set(int index, T value) {
		if(index < 0 || index >= length) throw new IndexOutOfBoundsException("index: " + index);
		ar[index] = value;
	}

	public int size() { return length; }

	public void add(T value) {
		if(length == capacity) {
			resize((int)(capacity * LOAD_FACTOR) + 1);
		}
		ar[length] = value;
		length++;
	}

	public void display() {
		System.out.print("[ ");
		for(int i = 0; i < length; i++) {
			System.out.print(ar[i] + " ");
		}
		for(int i = length; i < capacity; i++) {
			System.out.print("_ ");	
		}
		System.out.println(" ]");
	}
}


class VectorDemo {
	public static void main(String args[]) {
		Vector<Integer> vec = new Vector(1, 10);
		
		for(int i = 0; i < 9; i++) {
			vec.display();
			vec.add(i);
		}
		vec.display();
	}
}
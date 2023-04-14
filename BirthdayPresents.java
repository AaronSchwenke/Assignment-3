import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.lang.System;
import java.lang.Math;
import java.util.*;

public class BirthdayPresents {
	// numGifts is only 50,000 because my implementation already takes around 5 seconds to complete only 50,000 gifts.
	public static int numThreads = 4, numGifts = 50000, restricted = 0;
	public static Servant[] servants = new Servant[numThreads];
	public static Thread[] threads = new Thread[numThreads];
	public static ArrayList<Integer> giftsInBag = new ArrayList<>();
    public static void main(String[] args) {
		for (int i=0; i<numGifts; i++) {
			giftsInBag.add(i);
		}
		Collections.shuffle(giftsInBag);

		double startTime = System.nanoTime() * 1E-9;

		for (int i=0; i<numThreads; i++) {
			servants[i] = new Servant();
			threads[i] = new Thread(servants[i]);
			threads[i].start();
		}

		for (int i=0; i<numThreads; i++) {
			try {
				threads[i].join();
			}
			catch (Exception e) {}
		}

		double endTime = System.nanoTime() * 1E-9;
		double executionTime = endTime - startTime;

		System.out.println(executionTime);
    }

	public static synchronized int getAndIncRestricted() {
		int res = BirthdayPresents.restricted++;
		return res;
	}
}

class Servant implements Runnable {
	public static volatile int presentsLeft = BirthdayPresents.numGifts, cardsLeft = BirthdayPresents.numGifts;
	public static LinkedListCon linkedList = new LinkedListCon();
	public static final int ADD_GIFT = 0;
	public static final int REMOVE_GIFT = 1;
	public static final int CHECK_GIFT = 2;
	public static Lock lock = new ReentrantLock();

	public Servant() {

	}

	public void run() {
		boolean presentsLeft = true;
		boolean cardsLeft = true;

		while (presentsLeft || cardsLeft) {
			//System.out.println("presentsleft: " + Servant.presentsLeft + " cardsleft: " + Servant.cardsLeft);
			int randomAction = (int)(Math.random() * 3);
			if (randomAction == REMOVE_GIFT && linkedList.keysInList.size() == 0)
				randomAction = ADD_GIFT;
			if (!presentsLeft && randomAction == ADD_GIFT)
				randomAction = REMOVE_GIFT;
			

			if (randomAction == ADD_GIFT) {
				presentsLeft = getAndDecrementPresentsLeft();
				if (!presentsLeft)
					continue;

				int restricted = BirthdayPresents.getAndIncRestricted();
				int key = BirthdayPresents.giftsInBag.get(restricted);
				linkedList.add(key);
			}
			else if (randomAction == REMOVE_GIFT) {
				int key = linkedList.getRandomKey();
				if (key == -1 && Servant.cardsLeft > 0)
					continue;
				cardsLeft = getAndDecrementCardsLeft();
				if (!cardsLeft)
					break;

					linkedList.remove(key);
			}
			else {
				int key = (int)(Math.random() * BirthdayPresents.numGifts);
				boolean listHasKey = linkedList.contains(key);
			}
		}
	}

	public boolean getAndDecrementCardsLeft() {
		lock.lock();
		try {
			int cardsLeft = Servant.cardsLeft--;
			if (cardsLeft <= 0)
				return false;
			else
				return true;
		}
		catch (Exception e) {
			e.printStackTrace();
			return getAndDecrementPresentsLeft();
		}
		finally {
			lock.unlock();
		}
	}

	public boolean getAndDecrementPresentsLeft() {
		lock.lock();
		try {
			int presentsLeft = Servant.presentsLeft--;
			if (presentsLeft <= 0)
				return false;
			else
				return true;
		}
		catch (Exception e) {
			e.printStackTrace();
			return getAndDecrementPresentsLeft();
		}
		finally {
			lock.unlock();
		}
	}
}

class LinkedListCon {
	public Node head;
	public ArrayList<Integer> keysInList = new ArrayList<>();
	public Lock listLock = new ReentrantLock();

	public LinkedListCon() {
		head = new Node(-1);
		head.next = new AtomicMarkableReference<Node>(new Node(Integer.MAX_VALUE), false);
	}

	public View find(int key) {
		Node pred = null, cur = null, succ = null;
		boolean[] marked = {false};
		boolean cut;

		outer: while (true) {
			pred = this.head;
			cur = pred.next.getReference();

			while (true) {
				succ = cur.next.get(marked);
				while (marked[0]) {
					cut = pred.next.compareAndSet(cur, succ, false, false);
					if (!cut) 
						continue outer;
					cur = succ;
					succ = cur.next.get(marked);
				}

				if (cur.key >= key)
					return new View(pred, cur);
				pred = cur;
				cur = succ;
			}
		}
	}

	public boolean remove(int key) {
		Boolean cut;
		while (true) {
			View view = find(key);
			Node pred = view.pred, cur = view.cur;

			if (cur.key != key)
				return false;
			else {
				Node succ = cur.next.getReference();
				cut = cur.next.compareAndSet(succ, succ, false, true);
			
				if (!cut)
					continue;
				pred.next.compareAndSet(cur, succ, false, false);

				listLock.lock();
				try {
					keysInList.remove(keysInList.indexOf(Integer.valueOf(key)));
				}
				finally {
					listLock.unlock();
				}
				return true;
			}
		}
	}

	public boolean add(int key) {
		while (true) {
			View view = find(key);
			Node pred = view.pred, cur = view.cur;

			if (cur.key == key)
				return false;
			else {
				Node newNode = new Node(key);
				newNode.next = new AtomicMarkableReference<Node>(cur, false);

				if (pred.next.compareAndSet(cur, newNode, false, false)) {
					listLock.lock();
					try {
						keysInList.add(Integer.valueOf(key));
					}
					finally {
						listLock.unlock();
					}
					return true;
				}
			}
		}
	}

	public boolean contains(int key) {
		boolean[] marked = new boolean[1];
		Node cur = this.head;

		while (cur.key < key)
			cur = cur.next.getReference();
		Node succ = cur.next.get(marked);
		if (cur.key == key && !marked[0])
			return true;
		else
			return false;
	}

	public int getRandomKey() {
		listLock.lock();
		try {
			int listSize = keysInList.size();
			if (listSize == 0)
				return -1;
			return (int)(Math.random() * listSize);
		}
		finally {
			listLock.unlock();
		}
	}
}

class View {
	public Node pred, cur;

	public View(Node pred, Node cur) {
		this.pred = pred;
		this.cur = cur;
	}
}

class Node {
	int key;
	AtomicMarkableReference<Node> next;

	public Node(int key) {
		this.key = key;
		this.next = new AtomicMarkableReference<Node>(null, false);
	}
}
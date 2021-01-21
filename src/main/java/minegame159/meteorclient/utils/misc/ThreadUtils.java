package minegame159.meteorclient.utils.misc;

public class ThreadUtils {

	/**
	 * Runs a method in a thread. The method cannot return anything. A stack trace is dumped if an exception occurs
	 * @param method the method to run
	 */
	public static void runInThread(Runnable method)
	{
		new Thread(() -> {
			try {
				method.run();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}

	/**
	 * Makes a thread delay for the specified time.
	 * @param ms Time to sleep in milliseconds
	 */
	public static void sleep(int ms)
	{
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

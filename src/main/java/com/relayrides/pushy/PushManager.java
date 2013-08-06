package com.relayrides.pushy;

import java.lang.ref.WeakReference;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class PushManager<T extends ApnsPushNotification> {
	private final BlockingQueue<T> queue;
	
	private final ApnsEnvironment environment;
	private final KeyStore keyStore;
	private final char[] keyStorePassword;
	
	private final ApnsClientThread<T> clientThread;
	
	private final ArrayList<WeakReference<FailedDeliveryListener<T>>> failedDeliveryListeners;
	
	public PushManager(final ApnsEnvironment environment, final KeyStore keyStore, final char[] keyStorePassword) {
		this.queue = new LinkedBlockingQueue<T>();
		this.failedDeliveryListeners = new ArrayList<WeakReference<FailedDeliveryListener<T>>>();
		
		this.environment = environment;
		
		this.keyStore = keyStore;
		this.keyStorePassword = keyStorePassword;
		
		this.clientThread = new ApnsClientThread<T>(this);
	}
	
	public ApnsEnvironment getEnvironment() {
		return this.environment;
	}
	
	public KeyStore getKeyStore() {
		return this.keyStore;
	}
	
	public char[] getKeyStorePassword() {
		return this.keyStorePassword;
	}
	
	public synchronized void start() {
		this.clientThread.start();
	}
	
	public void enqueuePushNotification(final T notification) {
		this.queue.add(notification);
	}
	
	public void enqueueAllNotifications(final Collection<T> notifications) {
		this.queue.addAll(notifications);
	}
	
	public synchronized List<T> shutdown() throws InterruptedException {
		this.clientThread.shutdown();
		this.clientThread.join();
		
		return new ArrayList<T>(this.queue);
	}
	
	public void registerFailedDeliveryListener(final FailedDeliveryListener<T> listener) {
		this.failedDeliveryListeners.add(new WeakReference<FailedDeliveryListener<T>>(listener));
	}
	
	protected void notifyListenersOfFailedDelivery(final T notification, final Throwable cause) {
		for (final WeakReference<FailedDeliveryListener<T>> listenerReference : this.failedDeliveryListeners) {
			final FailedDeliveryListener<T> listener = listenerReference.get();
			
			if (listener != null) {
				listener.handleFailedDelivery(notification, cause);
			}
		}
	}
	
	protected BlockingQueue<T> getQueue() {
		return this.queue;
	}
	
	public List<ExpiredToken> getExpiredTokens() throws InterruptedException {
		return new FeedbackServiceClient(this).getExpiredTokens();
	}
}
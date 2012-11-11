package org.pepit;

import android.content.Context;

/*
 * Méthodes à implémenter par les plugins.
 */

public interface PluginInterface {
	public void method1(String message);
	public void method2(String message, Context ctx);
}

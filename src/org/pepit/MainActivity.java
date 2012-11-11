package org.pepit;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import android.os.Bundle;
import android.os.AsyncTask;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.content.Context;
import android.util.Log;

import dalvik.system.DexClassLoader;

/*
 * Démo téléchargement plugin par HTTP
 * 
 * Le mécanisme utilisé provient de : 
 *    http://android-developers.blogspot.fr/2011/07/custom-class-loading-in-dalvik.html
 *    http://code.google.com/p/android-custom-class-loading-sample/
 *    
 * Pour le téléchargement par HTTP, veiller à avoir dans le AndroidManifest.xml la permission : 
 *   <uses-permission android:name="android.permission.INTERNET"/>
 *   
 * Le fichier .apk du plugin est un projet Android indépendant sans Activity.
 * Ce fichier est généré lors de la compilation dans le sous-répertoire "bin/".
 * La classe du plugin qui sera instanciée dynamiquement doit implémenter l'interface PluginInterface.
 *   
 */

public class MainActivity extends Activity {

	// Bouton de test pour lancer la démo plugin
	private Button pluginTestButton;
	// Nom du fichier plugin, ici un projet Android compilé indépendamment
	private static final String PLUGIN1_NAME = "plugin1.apk";
	// Nom de la classe du plugin à instancier
	private static final String PLUGIN1_CLASS = "pluginC";
	// URL où télécharger le plugin. L'adresse 10.0.2.2 permet d'accéder au localhost depuis l'émulateur Android
	private static final String PLUGIN1_URL = "http://10.0.2.2/" + PLUGIN1_NAME;
	
	// Nom du fichier plugin, ici un projet Android compilé indépendamment
	private static final String PLUGIN2_NAME = "plugin2.apk";
	// Nom de la classe du plugin à instancier
	private static final String PLUGIN2_CLASS = "packagePlugin2.plugin2C";
	// URL où télécharger le plugin. L'adresse 10.0.2.2 permet d'accéder au localhost depuis l'émulateur Android
	private static final String PLUGIN2_URL = "http://10.0.2.2/" + PLUGIN2_NAME;
	
	// Classe pour la gestion du téléchargement du plugin
	// /!\ : ce téléchargement _doit_ se faire de manière asynchrone pour éviter une erreur "StrictMode" !
	//       voir : http://www.vogella.com/articles/AndroidNetworking/article.html
	class DownloadFileAsync extends AsyncTask<String, String, String> {
		
		// Indique le fichier destination de la copie via HTTP
		private File file = null;
		
		// Nom de la classe du plugin à instancier
		private String classname = "";
		
		// Contexte courant
		private Context ctx = null;
		
		public void setContext(Context c) {
			this.ctx = c;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		// Gestion du téléchargement du plugin en asynchrone
		@Override
		protected String doInBackground(String... aurl) {
			int count;
			try {
				URL url = new URL(aurl[0]);
				this.file = new File(aurl[1]);
				this.classname = aurl[2];
				URLConnection connexion = url.openConnection();
				connexion.connect();

				int lenghtOfFile = connexion.getContentLength();
				Log.d("plugin", "Lenght of file: " + lenghtOfFile);

				InputStream input = new BufferedInputStream(url.openStream());
				OutputStream output = new FileOutputStream(this.file.getAbsolutePath());

				byte data[] = new byte[8096];
				long total = 0;
				while ((count = input.read(data)) != -1) {
					total += count;
					publishProgress(""+(int)((total*100)/lenghtOfFile));
					output.write(data, 0, count);
				}
				output.flush();
				output.close();
				input.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
		
		protected void onProgressUpdate(String... progress) {
			 Log.d("plugin", "Run onProgressUpdate(): " + (Integer.parseInt(progress[0])));
		}

		// Une fois le plugin chargé, on peut appeler la méthode prédéfinie du plugin
		@Override
		protected void onPostExecute(String unused) {
			Log.d("plugin", "Run onPostExecute()");
			
			try {
				final File optimizedDexOutputPath = getDir("outdex",
						Context.MODE_PRIVATE);
				// On charge le plugin 
				DexClassLoader cl = new DexClassLoader(
						this.file.getAbsolutePath(),
						optimizedDexOutputPath.getAbsolutePath(), null,
						getClassLoader());
				// On instancie la classe du plugin
				Class<?> libClass = cl.loadClass(this.classname);
				PluginInterface lib = (PluginInterface) libClass
						.newInstance();
				lib.method1("Hello to plugin by calling method1()");
				lib.method2("Hello to plugin by calling method2()", this.ctx);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
	}

	// Traitement associé au bouton
	private void testPlugin() {
		Log.d("plugin", "Run testPlugin()");

		// Fichier local où sera stocké le plugin téléchargé par HTTP (plugin1)
		final File dexInternalStoragePath1 = new File(getDir("dex",
				Context.MODE_PRIVATE), PLUGIN1_NAME);
		// Fichier local où sera stocké le plugin téléchargé par HTTP (plugin2)
				final File dexInternalStoragePath2 = new File(getDir("dex",
						Context.MODE_PRIVATE), PLUGIN2_NAME);
		// TODO: Pour l'instant, on télécharge à chaque fois le plugin
		//       Il serait plus judicieux de le télécharger uniquement si besoin
		//       -> vérifier qu'il n'existe pas déjà en local, qu'il n'a pas été mis à jour côté serveur...
		DownloadFileAsync dfa1 = new DownloadFileAsync();
		dfa1.setContext(this.getApplicationContext());
		dfa1.execute(PLUGIN1_URL, dexInternalStoragePath1.getAbsolutePath(), PLUGIN1_CLASS);
		// 2ème plugin
		DownloadFileAsync dfa2 = new DownloadFileAsync();
		dfa2.setContext(this.getApplicationContext());
		dfa2.execute(PLUGIN2_URL, dexInternalStoragePath2.getAbsolutePath(), PLUGIN2_CLASS);
	
		/*
		 * TREST UTILE POUR LISTER LES CLASSES DANS LE FICHIER APK : 
		 * File optimizedDexOutputPath = getDir("outdex", Context.MODE_PRIVATE);
		 * DexFile dexFile = DexFile.loadDex(dexInternalStoragePath.getAbsolutePath(),
		 * 	optimizedDexOutputPath.getAbsolutePath() + "/outputdexcontainer.dex", 0);
		 * Enumeration<String> classFileNames = dexFile.entries();
		 * while (classFileNames.hasMoreElements()) {
		 * 	String className = classFileNames.nextElement();
		 * 	Log.d("plugin", "ClassName: " + className);
		 *  // // dexFile.loadClass(className, getClassLoader());
		 * }
		 */
	}

	private void attachClickTestButton() {
		pluginTestButton = (Button) findViewById(R.id.pluginTestButton);
		pluginTestButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				testPlugin();
			}
		});
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// bind test button to method
		attachClickTestButton();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
}

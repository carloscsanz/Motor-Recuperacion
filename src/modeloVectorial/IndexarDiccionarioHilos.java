/* Realizado por Carlos Contreras Sanz (100303562) y Miguel Xoel García Balsa (100291036) */
package modeloVectorial;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;

public class IndexarDiccionarioHilos {
	
	public static final String rutaDocumentos = "./Documentos/"; //Ruta en la que se encuentran guardados los documentos .html
	public static final String rutaNombresMasculinos = "./Documentos/male_v2.txt";
	public static final String rutaNombresFemeninos = "./Documentos/female.txt";
	
	public static Map<String, String> hashDocumentos;
	public static ArrayList<String> Documentos;
	public static List<String> nombresEntidades;
	public static ArrayList<InsercionMongoHilos> hilosEjec = new ArrayList<InsercionMongoHilos>();

	public static void main(String[] args) {
		
		/* Obtenemos los documentos y el hashMap de ellos para poder identificarlos por un numero unico */
		Documentos doc = new Documentos();
		hashDocumentos = doc.importarDocumentos(rutaDocumentos);
		Documentos = doc.getNombreDocumentos();
		
		/* Obtenemos los nombres de entidades de los documentos */
		List<String> nombresMasculinos = Arrays.asList(doc.abrirFichero(rutaNombresMasculinos).split(" "));
		List<String> nombresFemeninos = Arrays.asList(doc.abrirFichero(rutaNombresFemeninos).split(" "));

		List<String> nombresEntidades = Stream.concat(nombresMasculinos.stream(), nombresFemeninos.stream()).collect(Collectors.toList());
		
		// PASO 1: Conexión al Server de MongoDB Pasandole el host y el puerto
		MongoClient mongoClient = new MongoClient("localhost", 27017);

		// PASO 2: Conexión a la base de datos
		@SuppressWarnings("deprecation")
		DB db = mongoClient.getDB("motor");

		// PASO 3: Obtenemos una coleccion para trabajar con ella
		DBCollection coleccionDiccionario = db.getCollection("Diccionario");
		DBCollection coleccionEntidades = db.getCollection("Entidades");
		
		//PASO 4: Creamos los indices sobre los campos por los que insertaremos
		db.getCollection("Diccionario").createIndex("palabra");
		db.getCollection("Entidades").createIndex("entidad");
		
		/* Añadimos los terminos de cada uno de los documentos al diccionario */
		if(!hashDocumentos.isEmpty()){
			for(int i = 0; i< hashDocumentos.size() ; i++){
				
				InsercionMongoHilos hilo1 = new InsercionMongoHilos(hashDocumentos.get(Documentos.get(i)), Documentos.get(i), nombresEntidades, coleccionDiccionario, coleccionEntidades);				

				if(hilosEjec.size() < 4){
					
					hilosEjec.add(hilo1);
					
					hilo1.start();
										
				}else{
					
					while(hilosEjec.size() <= 4 && hilosEjec.size() > 0){
						for(int ii=0;ii<hilosEjec.size();ii++){
							if(!hilosEjec.get(ii).isAlive()){
								hilosEjec.remove(ii);
							}
						}
					}
					
					hilosEjec.add(hilo1);
					hilo1.start();
					
				}

			}
			
			for(int ii=0;ii<hilosEjec.size();ii++){
				try {
					hilosEjec.get(ii).join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
		
		//PASO FINAL: Cerrar la conexion
		mongoClient.close();
				
	}

}

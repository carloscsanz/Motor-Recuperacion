/* Realizado por Carlos Contreras Sanz (100303562) y Miguel Xoel García Balsa (100291036) */
package modeloVectorial;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Modelos {
	
	public static final String rutaDocumentos = "./Documentos/"; //Ruta en la que se encuentran guardados los documentos .html
	public static final String rutaConsultas = "./Documentos/2010-topics.xml";
	public static final String rutaNombresMasculinos = "./Documentos/male_v2.txt";
	public static final String rutaNombresFemeninos = "./Documentos/female.txt";
	
	public static Map<String, String> consultas;
	public static ArrayList<String> nombresConsultas = new ArrayList<String>();
	public static Map<String, Map<String, Integer>> Diccionario = new HashMap<String, Map<String, Integer>>();
	public static List<String> Entidades = new ArrayList<String>();
	public static Map<String, String> hashDocumentos;
	public static ArrayList<String> Documentos;
	
	public static void main(String[] args) throws IOException {
		
		// Leemos las consultas del .xml
		consultas = leerConsultas(rutaConsultas);
		
		// Obtenemos los documentos y el List de ellos para poder identificarlos por su nombre
		Documentos doc = new Documentos();
		hashDocumentos = doc.importarDocumentos(rutaDocumentos);
		Documentos = doc.getNombreDocumentos();
				
		// PASO 1: Conexión al Server de MongoDB Pasandole el host y el puerto
		MongoClient mongoClient = new MongoClient("localhost", 27017);

		// PASO 2: Conexión a la base de datos
		@SuppressWarnings("deprecation")
		DB db = mongoClient.getDB("motor");

		// PASO 3: Obtenemos una coleccion para trabajar con ella
		DBCollection collectionDiccionario = db.getCollection("Diccionario");
		DBCollection collectionEntidades = db.getCollection("Entidades");

		DBCursor cursorDiccionario = collectionDiccionario.find();
		DBCursor cursorEntidades = collectionEntidades.find();

		//Recuperamos de la BBDD el Diccionario y Entidades
		hashDiccionario(cursorDiccionario);
		hashEntidades(cursorEntidades);
		
		// PASO FINAL: Cerrar la conexion
		mongoClient.close();
		
		Diccionario diccionario = new Diccionario();
		Limpieza limpieza = new Limpieza();
		Calculos calculos = new Calculos();
		
		Date fechaActual = new Date();
		DateFormat formatoFecha = new SimpleDateFormat("dd_MM_HH:mm:ss");
		
		String aux = formatoFecha.format(fechaActual);
		
		File archivo = new File("./" + aux + "_Resultados.txt");
		BufferedWriter bw = new BufferedWriter(new FileWriter(archivo));
		
		for(int i=0 ; i<nombresConsultas.size() ; i++){
			
			Map<String, Map<String, Integer>> copiaDiccionario = Diccionario;
			
			String consulta = nombresConsultas.get(i);
			
			Map<String, Double> query = diccionario.diccionarioConsulta(limpieza.filtro(consultas.get(consulta)));
			
			Sinonimos sinonimos = new Sinonimos();
			Map<String, Double> querySin = sinonimos.encontrarSinonimos(query);
			
			// ENTIDADES UNICAMENTE SI SE INCLUYE EL NOMBRE EN LA QUERY
			
			List<String> entidadesQuery = contieneEntidad(querySin);
			
			if(!entidadesQuery.isEmpty()){
				
				for(int ii=0;ii<entidadesQuery.size();ii++){
					
					if(copiaDiccionario.containsKey(entidadesQuery.get(ii))){
						
						Map<String, Integer> docs = copiaDiccionario.get(entidadesQuery.get(ii));
						
						for (Entry<String, Integer> entry : docs.entrySet()){
							
							docs.put(entry.getKey(), entry.getValue() * 30 );
							
						}
						
						copiaDiccionario.put(entidadesQuery.get(ii), docs);
						
					}

				}

			}
			
			Map<String, Double> IDF = calculos.calcularIDF(1967, copiaDiccionario, querySin);
			Map<String, Double> TFIDF = calculos.ProductoEscalarTFIDF(copiaDiccionario, querySin, IDF, Documentos);
			Map<String, Double> cosTFIDF = calculos.CosenoTFIDF(copiaDiccionario, TFIDF, IDF, querySin);
			
			OrdenarHashMap ordenado = new OrdenarHashMap(cosTFIDF); 
			
			TreeMap<String, Double> sorted_map = new TreeMap<String, Double>(ordenado);
			sorted_map.putAll(cosTFIDF);
			
			for (Entry<String, Double> entry : sorted_map.entrySet()){
				
				bw.write(consulta + "\t" + entry.getKey() + "\t\t" + entry.getValue() + "\n");
				
			}
			
			bw.write("\n" + "-------------------------------------------------" + "\n\n");
			
		}
		
		bw.close();
		System.out.println("\nFichero creado, refresca el explorador de Eclipse para ver los resultados en ./resultados.txt");
		
	}
	
	public static Map<String, String> leerConsultas(String ruta) throws IOException{
		
		Document doc = (Document) Jsoup.parse(new File(ruta), "UTF-8");
		
		Elements paragraphs = ((Element) doc).select("body");
		Map<String, String> resultado = new HashMap<String, String>();

		for (Element p : paragraphs) {
		    
			Elements links =  p.getElementsByAttribute("id");
			
		    for (Element link : links) {
		    	
		        String linkText = link.getElementsByTag("title").text();
		        String topicTag = link.attr("id");
		        resultado.put(topicTag, linkText);
		        nombresConsultas.add(topicTag);
		        
		    }
		}
		return resultado;
	}

	public static void hashDiccionario(DBCursor cursor){
				
		while(cursor.hasNext()){
			
			DBObject it = cursor.next();
			
			String palabra = (String) it.get("palabra");
			Map<String, Integer> docs = new HashMap<String, Integer>();
			Map<?, ?> objetoMapeado = it.toMap();
			
			objetoMapeado.remove("_id");
			objetoMapeado.remove("palabra");
			
			for(int i=0 ; i<objetoMapeado.keySet().toArray().length ; i++){
				docs.put( (String) objetoMapeado.keySet().toArray()[i] , (int) objetoMapeado.get(objetoMapeado.keySet().toArray()[i]));
			}
			
			Diccionario.put(palabra, docs);
		}
		
	}
	
	public static void hashEntidades(DBCursor cursor){
		
		ArrayList<String> entidades = new ArrayList<String>();
		
		while(cursor.hasNext()){
			
			DBObject it = cursor.next();
			entidades.add( (String) it.get("entidad"));
			
		}
		
		Entidades = entidades;
		
	}

	public static List<String> contieneEntidad(Map<String, Double> query){
		
		List<String> entidadesDoc = 
				query.keySet()
				.stream()
				.filter(e -> Entidades.contains(e))
				.collect(Collectors.toList());
		
		return entidadesDoc;
		
	}

}
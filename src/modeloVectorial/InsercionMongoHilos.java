/* Realizado por Carlos Contreras Sanz (100303562) y Miguel Xoel García Balsa (100291036) */
package modeloVectorial;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.StringTokenizer;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class InsercionMongoHilos extends Thread{
	
	public String rutaDocumento;	
	public String nombreDocumento;
	
	public DBCollection coleccionDiccionario;
	public DBCollection coleccionEntidades;
	
	public List<String> entidades;
	public boolean tieneEntidades = false;

	public InsercionMongoHilos(String rutaDocumento, String nombreDocumento, List<String> entidades, DBCollection coleccionDiccionario, DBCollection coleccionEntidades) {
		this.coleccionDiccionario = coleccionDiccionario;
		this.coleccionEntidades = coleccionEntidades;
		this.rutaDocumento = rutaDocumento;
		this.nombreDocumento = nombreDocumento;
		this.entidades = entidades;
	}
		
	public Map<String, Integer> crearDiccionarioDocumento(StringTokenizer textoLimpio){
		
		Map<String, Integer> diccionarioDocumento = new HashMap<String, Integer>();
		
		while(textoLimpio.hasMoreTokens()){
			String palabra = textoLimpio.nextToken();
			
			if(diccionarioDocumento.containsKey(palabra)){
				diccionarioDocumento.put(palabra, diccionarioDocumento.get(palabra)+1);
			}else{
				diccionarioDocumento.put(palabra, 1);
			}
			
			if(entidades.contains(palabra) && !tieneEntidades){
				tieneEntidades = true;
			}
		}
		
		return diccionarioDocumento;
	}

	@Override
	public void run() {
												
		Documentos doc = new Documentos();
		Limpieza limpiezaTexto = new Limpieza();
			
		//OBTENGO LOS DATOS DEL DOCUMENTO
		String texto = doc.abrirFichero(this.rutaDocumento);
			
		//LIMPIO LOS DATOS DEL DOCUMENTO
		StringTokenizer textoLimpio = limpiezaTexto.filtro(texto);
		
		//AÑADO LOS DATOS AL DICCIONARIO
		Map<String, Integer> diccionarioDocumento = crearDiccionarioDocumento(textoLimpio);
		
		for (Entry<String, Integer> entry : diccionarioDocumento.entrySet()){
					
			BasicDBObject query = new BasicDBObject("palabra", entry.getKey());
			DBObject updated = new BasicDBObject().append("$set", new BasicDBObject().append(this.nombreDocumento, entry.getValue()));
			
			synchronized(coleccionDiccionario){
				
				coleccionDiccionario.update(query, updated, true, false);
				
			}
				
		}	
		
		System.out.println("Documento "+ this.nombreDocumento + " insertado");
		
		if(tieneEntidades){
			
			List<String> entidadesDoc = 
					diccionarioDocumento.keySet()
					.stream()
					.filter(e -> entidades.contains(e))
					.collect(Collectors.toList());
			
			for (int i=0 ; i<entidadesDoc.size() ; i++){
				
				BasicDBObject query = new BasicDBObject("entidad", entidadesDoc.get(i));
				DBObject updated = new BasicDBObject().append("$set", new BasicDBObject().append("documento", nombreDocumento));
				
				synchronized(coleccionEntidades){
					
					coleccionEntidades.update(query, updated, true, false);
					
				}
					
			}
					
		}

	}
	
}

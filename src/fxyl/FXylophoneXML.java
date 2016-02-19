package fxyl;

//<editor-fold defaultstate="collapsed" desc="Imports.">
import constants.Constants;
import exc.WrongNoteException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import note.Note;
import note.NoteComparator;
import note.NoteImpl;
import note.NoteQueue;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
//</editor-fold>

/**
 * Classe que implementa la inserció i extracció d'objectes Note a un fitxer 
 * XML. 
 *
 * 020216
 * @author mor
 */
public class FXylophoneXML {
    
    // PROPIETATS
    
    //<editor-fold defaultstate="collapsed" desc="Propietats.">
    /**
     * Estructura de dades per guardar temporalment objectes Note en el procés
     * de grabació/extracció de notes.
     */
    private List<Note> noteList = new NoteQueue<>();
    /**
     * Objecte que implementa Comparator per ordenar adequadament, segons
     * el seu timestamp, els objectes Note a la seva estructura de dades.
     */
    private NoteComparator nc = new NoteComparator();
    /**
     * Nom del fitxer xml en el qual es guardaran les dades dels objectes
     * Note (sense extensió!).
     */
    private String fileName;
    //</editor-fold>
    
    // MÈTODES
    
    //<editor-fold defaultstate="collapsed" desc="Constructors.">
    /**
     * Constructor sense arguments.
     */
    public FXylophoneXML() { }
//</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="Mètodes Note -> XML.">
    /**
     * Funció que construeix un element <Note>.
     * Per tal de poder guardar les dades dels missatges MIDI a un XML, cal
     * convertir-les a un format XML. Aquesta funció s'encarrega de crear un
     * element XML a partir d'una nota enviat per paràmetre.
     *
     * @param n Objecte de la classe Note.
     * @param doc Objecte Document al qual s'afegeix l'element.
     * @return Element
     */
    public Element XMLcreateNoteElement(Note n, Document doc) {
        
        Element note = doc.createElement(Constants.NOTE_ELEMENT);
        
        /**
         * Elements fills de <Note>
         */
        Element value = doc.createElement(Constants.VALUE_ELEMENT);
        value.appendChild(
                doc.createTextNode(Integer.toString(n.getValue())));
        Element timestamp = doc.createElement(Constants.TIMESTAMP_ELEMENT);
        timestamp.appendChild(
                doc.createTextNode(Long.toString(n.getTimestamp())));
        
        note.appendChild(value);
        note.appendChild(timestamp);
        
        return note;
        
    }
    
    /**
     * Funció Note -> XML.
     * Guarda les dades d'un conjunt d'objectes Note en un document XML
     * utilitzant l'API DOM.
     *
     * @throws ParserConfigurationException
     * @throws WrongNoteException
     * @throws TransformerException
     */
    public void notesToXML() throws ParserConfigurationException,
            WrongNoteException, TransformerException {
        
        noteList.sort(nc);
        
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        
        Document docNode = docBuilder.newDocument();
        Element rootElement = docNode.createElement(Constants.SOUNDFILE_ELEMENT);
        docNode.appendChild(rootElement);
        
        for (Note note : noteList) {
            
            /**
             * Si no és una nota de MIDI, llença una excepció.
             */
            if(!(note instanceof Note))
                throw new WrongNoteException();
            
            Note noteToRecord = (Note) note;
            
            rootElement.appendChild(
                    XMLcreateNoteElement(noteToRecord, docNode)
            );
            
        }
        
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transf = tf.newTransformer();
        DOMSource orig = new DOMSource(docNode);
        StreamResult outXML = new StreamResult(new File(this.fileName));
        
        transf.transform(orig, outXML);
        
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="Mètodes XML -> Note.">
    public boolean recordNote(Note n) {
        
        System.out.println("XMLtoNote");
        System.out.println(n);
        if(!noteList.add(n))
            return false;
        
        return true;
        
    }
    
    /**
     * Funció per extreure un conjunt de notes d'un fitxer XML.
     * Per reproduir les notes guardades a un arxiu XML s'han de desar en una
     * llista d'objectes Note.
     * 
     * @return
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws WrongNoteException 
     */
    public List<Note> XMLtoNotes() throws IOException, SAXException, 
            ParserConfigurationException, WrongNoteException {
        
        File midiXml = new File(this.getFileName());
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(midiXml);
        doc.getDocumentElement().normalize();

        NodeList nodes = doc.getElementsByTagName(Constants.NOTE_ELEMENT);
        
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                
                Element element = (Element) node;
                Note noteToPlay = new NoteImpl(
                        Integer.parseInt(getContent(Constants.VALUE_ELEMENT, element)),
                        Long.parseLong(getContent(Constants.TIMESTAMP_ELEMENT, element))
                );
                
                if(!recordNote(noteToPlay))
                    throw new WrongNoteException();
                
            }
        }
        
        noteList.sort(nc);
        
        for (Note note : noteList)
            System.out.println(note);
        
        return noteList;
        
    }
    
    /**
     * Funció per recuperar el valor d'un node XML.
     * 
     * @param etiqueta Node que desitgem extreure de l'XML.
     * @param element Element a on cercar el node
     * @return El valor del node en forma de String.
     */
    private static String getContent(String etiqueta, Element element) {
        NodeList nodes = element.getElementsByTagName(etiqueta).item(0).getChildNodes();
        Node node = (Node) nodes.item(0);
        return node.getNodeValue();
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="Getters i setters">
    /**
     * Retorna l'estructura de dades que guarda objectes Note per grabar i
     * reproduir.
     * 
     * @return Llista d'objectes de la classe Note.
     */
    public List<Note> getNoteRecording() {
        return noteList;
    }
    
    /**
     * Retorna el comparador d'objectes Note.
     * 
     * @return Objecte que implementa Comparator.
     */
    public NoteComparator getNc() {
        return nc;
    }
    
    /**
     * Retorna el nom del fitxer per guardar/reproduir.
     * 
     * @return String del nom del fitxer XML (sense extensió).
     */
    public String getFileName() {
        return fileName;
    }
    
    /**
     * Defineix l'objecte llista de notes.
     * 
     * @param noteRecording Llista d'objectes de la classe Note.
     */
    public void setNoteRecording(NoteQueue<Note> noteRecording) {
        this.noteList = noteRecording;
    }
    
    /**
     * Defineix el comparador d'objectes Note.
     * 
     * @param nc Objecte que implementa Comparator.
     */
    public void setNc(NoteComparator nc) {
        this.nc = nc;
    }
    
    /**
     * Defineix el nom del fitxer per guardar/reproduir notes.
     * 
     * @param fileName String del nom del fitxer XML (sense extensió).
     */
    public void setFileName(String fileName) {
        this.fileName = fileName + Constants.EXT;
    }
    //</editor-fold>
    
}

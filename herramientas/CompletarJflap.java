import grammar.*;
import grammar.parse.*;
import gui.grammar.*;
import gui.grammar.parse.*;
import gui.environment.*;
import file.XMLCodec;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;
import javax.imageio.ImageIO;

/** Uses the real JFLAP codec, parser, and Swing tree renderer. */
public class CompletarJflap {
 static String OUT=".";
 static final String[][] RULES={
  {"S","bpiLf"},{"L","IL"},{"L","I"},
  {"I","A"},{"I","Y"},{"I","D"},
  {"A","B"},{"A","C"},{"B","i=V#"},{"C","i=E#"},
  {"E","VOV"},{"V","i"},{"V","d"},
  {"O","+"},{"O","-"},{"O","*"},{"O","/"},{"O","%"},
  {"Y","si#"},{"D","Pi#"},{"P","n"},{"P","r"}};
 static Grammar make(String start){
  Grammar g=new UnrestrictedGrammar();
  for(String[] r:RULES)g.addProduction(new Production(r[0],r[0].equals("S")&&start.equals("I")?"I":r[1]));
  g.setStartVariable("S"); return g;
 }
 static Grammar save(String name,String start)throws Exception{
  File f=new File(OUT+"/"+name+".jff"); Grammar g=make(start);
  new XMLCodec().encode(g,f,new HashMap<>());
  Grammar read=(Grammar)new XMLCodec().decode(f,new HashMap<>());
  if(!read.getStartVariable().equals("S")||read.getProductions().length!=RULES.length||!GrammarChecker.isContextFreeGrammar(read))throw new Exception("Codec/grammar mismatch");
  return read;
 }
 static Object field(Object o,String name)throws Exception{
  for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(NoSuchFieldException e){}
  throw new NoSuchFieldException(name);
 }
 static void layout(Component c){if(c instanceof Container){((Container)c).doLayout();for(Component ch:((Container)c).getComponents())layout(ch);}}
 static void image(JComponent c,String name,int w,int h)throws Exception{
  c.setSize(w,h);layout(c);
  BufferedImage im=new BufferedImage(w*2,h*2,BufferedImage.TYPE_INT_RGB); Graphics2D x=im.createGraphics();
  x.setColor(Color.WHITE);x.fillRect(0,0,w*2,h*2);x.scale(2,2);
  x.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
  c.paint(x);x.dispose();ImageIO.write(im,"png",new File(OUT+"/evidencias/"+name+".png"));
 }
 static ParseNode parse(Grammar g,String word)throws Exception{
  BruteParser p=BruteParser.get(g,word);p.start();long end=System.currentTimeMillis()+15000;
  while(!p.isFinished()&&System.currentTimeMillis()<end)Thread.sleep(10);
  if(!p.isFinished()){p.pause();throw new Exception("Timeout "+word);}
  return p.getAnswer();
 }
 static void capture(Grammar g,String word,String name)throws Exception{
  ParseNode a=parse(g,word);if(a==null)throw new Exception("Rejected "+word);
  ArrayList<String> lines=new ArrayList<>();
  for(ParseNode n=a;n!=null;n=(ParseNode)n.getParent())lines.add(n.getDerivation());
  Collections.reverse(lines);Files.write(Path.of(OUT,"evidencias",name+"_derivacion.txt"),lines);
  SwingUtilities.invokeAndWait(()->{try{
   GrammarInputPane input=new GrammarInputPane(g);
   GrammarEnvironment env=new GrammarEnvironment(input);
   BruteParsePane pane=new BruteParsePane(env,g,null);
   pane.inputField.setText(word);
   UnrestrictedTreePanel tree=(UnrestrictedTreePanel)field(pane,"treePanel");
   tree.setAnswer(a);int i=0;while(!tree.next()&&++i<300){}
   if(i>=300)throw new Exception("Tree incomplete");
   image(tree,name+"_arbol",650,450);
   pane.setSize(950,680);layout(pane);
   for(String key:new String[]{"mainSplit","topSplit","bottomSplit"}){
    JSplitPane split=(JSplitPane)field(pane,key);if(split!=null)split.setDividerLocation(key.equals("mainSplit")?110:250);
   }
   image(pane,name+"_panel",950,680);
  }catch(Exception ex){throw new RuntimeException(ex);}});
  System.out.println("TREE "+name+" accepted "+word+" steps="+(lines.size()-1));
 }
 public static void main(String[] args)throws Exception{
  if(args.length>0)OUT=args[0];
  System.setProperty("java.awt.headless","true");new File(OUT+"/evidencias").mkdirs();
  Grammar main=save("Gramatica_Lab3","S"), inst=save("Gramatica_Lab3_Instrucciones","I");
  capture(inst,"i=i+i#","01_asignacion");
  capture(inst,"si#","02_salida");
  capture(inst,"ri#","03_declaracion");
  capture(main,"bpii=i+i#f","04_programa");
  String[] words={"i=d#","i=i+i#","si#","id#","i=+ii#","s#","i=d","bpii=d#si#f","ni#","ri#","n#","rii#"};
  PrintWriter test=new PrintWriter(OUT+"/evidencias/pruebas_jflap.csv","UTF-8");
  test.println("numero,inicio,entrada,esperado,obtenido");
  for(int i=0;i<words.length;i++){
   String w=words[i];boolean want=i<3||i==7||i==8||i==9;
   boolean got=parse(i==7?main:inst,w)!=null;
   test.println((i+1)+","+(i==7?"programa":"instruccion_aux")+","+w+","+want+","+got);
   if(want!=got)throw new Exception("Test mismatch "+w);
  }
  test.close();System.out.println("JFLAP_NATIVE_TESTS=12 PASS; CODEC=22 productions PASS");
  System.exit(0);
 }
}

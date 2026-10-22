"""Verificación reproducible de los JFF entregados. Python 3, sin dependencias."""
from pathlib import Path
import xml.etree.ElementTree as ET
import re, json, itertools, random, csv

ROOT=Path(__file__).resolve().parent

def read_jff(path):
    grammar={}
    for p in ET.parse(path).findall('production'):
        grammar.setdefault(p.findtext('left'),[]).append(tuple(p.findtext('right') or ''))
    return grammar

def accepts(g,tokens,start='S'):
    g={**g,'@':[(start,)]}
    chart=[set() for _ in range(len(tokens)+1)]
    chart[0].add(('@',(start,),0,0))
    for pos in range(len(chart)):
        while True:
            before=len(chart[pos])
            for lhs,rhs,dot,origin in list(chart[pos]):
                if dot==len(rhs):
                    for a,b,c,d in list(chart[origin]):
                        if c<len(b) and b[c]==lhs: chart[pos].add((a,b,c+1,d))
                elif rhs[dot] in g:
                    for prod in g[rhs[dot]]: chart[pos].add((rhs[dot],prod,0,pos))
                elif pos<len(tokens) and rhs[dot]==tokens[pos]:
                    chart[pos+1].add((lhs,rhs,dot+1,origin))
            if len(chart[pos])==before: break
    return ('@',(start,),1,0) in chart[-1]

KEYWORDS={'Begin':'b','End':'f','Programa':'p','salida':'s','entero':'n','real':'r'}
def lex(source):
    pattern=re.compile(r'/\*.*?\*/|[a-zA-Z][a-zA-Z0-9_]*|[0-9]+(?:\.[0-9]+)?|[+*/%=#-]',re.S)
    result=[]; pos=0
    for m in pattern.finditer(source):
        if source[pos:m.start()].strip(): raise ValueError('Carácter léxico inválido')
        x=m.group();pos=m.end()
        if x.startswith('/*'):continue
        result.append(KEYWORDS[x] if x in KEYWORDS else x if x in '+-*/%=#' else 'd' if x[0].isdigit() else 'i')
    if source[pos:].strip():raise ValueError('Carácter léxico inválido')
    return ''.join(result)

def oracle(t):
    return bool(re.fullmatch(r'(?:[snr]i#|i=[id]#|i=[id][+*/%\-][id]#)',t))

def main():
    g=read_jff(ROOT/'Gramatica_Lab3.jff')
    aux=read_jff(ROOT/'Gramatica_Lab3_Instrucciones.jff')
    assert len(g)==12 and sum(map(len,g.values()))==22
    assert aux['S']==[('I',)]
    assert {k:v for k,v in g.items() if k!='S'}=={k:v for k,v in aux.items() if k!='S'}
    valid=['si#','ni#','ri#']+['i='+v+'#' for v in 'id']
    valid+=['i='+a+o+b+'#' for a,o,b in itertools.product('id','+-*/%','id')]
    candidates=set(valid); alphabet='idbfpsnr=+-*/%#'
    for t in valid:
        for i in range(len(t)):
            candidates.add(t[:i]+t[i+1:])
            for x in alphabet:candidates.add(t[:i]+x+t[i+1:])
        for i in range(len(t)+1):
            for x in alphabet:candidates.add(t[:i]+x+t[i:])
    rng=random.Random(3)
    for _ in range(1000):candidates.add(''.join(rng.choice(alphabet) for _ in range(rng.randrange(9))))
    for t in candidates:assert accepts(aux,t)==oracle(t),t
    for a,b in itertools.product(valid,repeat=2):assert accepts(g,'bpi'+a+b+'f')
    reference=lex((ROOT/'programa_referencia.txt').read_text(encoding='utf-8'))
    assert accepts(g,reference)
    cases=['x = 10 #','z = x + y #','salida z #','x 10 #','z = + x y #','salida #','x = 20','Begin Programa prueba x = 5 # salida x # End']
    rows=[]
    for i,source in enumerate(cases,1):
        encoded=lex(source); got=accepts(g if i==8 else aux,encoded)
        assert got==(i in (1,2,3,8))
        rows.append({'caso':i,'fuente':source,'tokens':encoded,'aceptada':got})
    edges={
        'Begin Programa demo End':False,
        'Begin Programa demo /* solo comentario */ End':False,
        'Begin Programa demo entero x # End':True,
        'Begin Programa demo x = x + x + x # End':False,
        'Begin Programa demo salida 20 # End':False,
        'Begin Programa demo x = -1 # End':False,
        'Begin Programa demo x = 0 # End':True,
        'Begin Programa demo /* comentario\nmultilinea */ salida x # End':True,
        'Begin Programa demo real temperatura # End':True,
        'Begin Programa demo entero # End':False,
    }
    for s,want in edges.items():assert accepts(g,lex(s))==want,s
    # Cada instrucción aislada válida necesita encabezado/cierre para pertenecer a L(G).
    assert not accepts(g,lex('x = 20 #'))
    native=list(csv.DictReader((ROOT/'evidencias/pruebas_jflap.csv').open(encoding='utf-8')))
    assert len(native)==12 and all(r['esperado']==r['obtenido'] for r in native)
    stats={'producciones_por_archivo':22,'instrucciones_probadas':len(candidates),'aceptadas':sum(oracle(t) for t in candidates),'rechazadas':sum(not oracle(t) for t in candidates),'discrepancias':0,'programas_de_dos_instrucciones':len(valid)**2,'programa_referencia_aceptado':True,'tokens_programa_referencia':len(reference),'pruebas_nativas_jflap':len(native),'casos':rows,'casos_adicionales':len(edges)}
    (ROOT/'evidencias/verificacion.json').write_text(json.dumps(stats,ensure_ascii=False,indent=2),encoding='utf-8')
    (ROOT/'evidencias/programa_referencia_tokens.txt').write_text(reference+'\n',encoding='utf-8')
    print(json.dumps(stats,ensure_ascii=False,indent=2))

if __name__=='__main__':main()

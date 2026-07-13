import { AppLayout } from "@/components/layout";
import { useIniciarConciliacion, useImportarMovimientos, useCruzarConciliacion } from "@/hooks/use-conciliacion";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useToast } from "@/hooks/use-toast";
import { useState } from "react";
import { FileSpreadsheet, Loader2, Upload, PlayCircle } from "lucide-react";

export default function Conciliacion() {
  const [cuentaId, setCuentaId] = useState("");
  const [periodo, setPeriodo] = useState("");
  const [conciliacionId, setConciliacionId] = useState<string | null>(null);
  const [fileContent, setFileContent] = useState("");
  
  const iniciar = useIniciarConciliacion();
  const importar = useImportarMovimientos();
  const cruzar = useCruzarConciliacion();
  const { toast } = useToast();

  const handleIniciar = () => {
    if (!cuentaId || !periodo) return;
    iniciar.mutate({ cuentaId, periodo }, {
      onSuccess: (data) => {
        setConciliacionId(data.id);
        toast({ title: "Conciliación iniciada", description: `ID: ${data.id}` });
      }
    });
  };

  const handleImportar = () => {
    if (!conciliacionId || !fileContent) return;
    
    try {
      // Very naive CSV parsing for demonstration
      const movimientos = fileContent.split('\n').filter(l => l.trim()).map(line => {
        const [fecha, monto, referencia, descripcionBanco] = line.split(',');
        return { fecha, monto: Number(monto), referencia, descripcionBanco };
      });

      importar.mutate({ id: conciliacionId, movimientos }, {
        onSuccess: () => {
          toast({ title: "Movimientos importados con éxito" });
          setFileContent("");
        }
      });
    } catch (e) {
      toast({ variant: "destructive", title: "Error de formato", description: "El CSV debe ser: fecha,monto,referencia,desc" });
    }
  };

  const handleCruzar = () => {
    if (!conciliacionId) return;
    cruzar.mutate(conciliacionId, {
      onSuccess: () => {
        toast({ title: "Cruce finalizado", description: "Los movimientos han sido conciliados." });
        setConciliacionId(null);
      }
    });
  };

  return (
    <AppLayout>
      <div className="flex flex-col gap-6 max-w-4xl">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Conciliación Bancaria</h1>
          <p className="text-muted-foreground mt-1">Cruce automático de extractos bancarios con libros contables.</p>
        </div>

        <div className="grid gap-6 md:grid-cols-3">
          <Card className={!conciliacionId ? "border-primary shadow-md" : "opacity-50"}>
            <CardHeader>
              <CardTitle className="text-lg flex items-center gap-2"><div className="bg-primary/10 p-2 rounded-full text-primary">1</div> Iniciar</CardTitle>
              <CardDescription>Selecciona cuenta y periodo</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label>ID Cuenta Bancaria</Label>
                <Input value={cuentaId} onChange={e => setCuentaId(e.target.value)} disabled={!!conciliacionId} placeholder="Ej. ACC-1234" />
              </div>
              <div className="space-y-2">
                <Label>Periodo (YYYY-MM)</Label>
                <Input type="month" value={periodo} onChange={e => setPeriodo(e.target.value)} disabled={!!conciliacionId} />
              </div>
              <Button className="w-full" onClick={handleIniciar} disabled={!cuentaId || !periodo || !!conciliacionId || iniciar.isPending}>
                {iniciar.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                Crear Sesión
              </Button>
            </CardContent>
          </Card>

          <Card className={conciliacionId ? "border-primary shadow-md" : "opacity-50 pointer-events-none"}>
            <CardHeader>
              <CardTitle className="text-lg flex items-center gap-2"><div className="bg-primary/10 p-2 rounded-full text-primary">2</div> Importar</CardTitle>
              <CardDescription>Carga el extracto bancario CSV</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="border-2 border-dashed rounded-lg p-6 flex flex-col items-center justify-center text-center text-muted-foreground bg-muted/20">
                <FileSpreadsheet className="h-8 w-8 mb-2 opacity-50" />
                <p className="text-xs">Pega el contenido CSV (fecha,monto,ref,desc)</p>
              </div>
              <textarea 
                className="w-full text-xs font-mono p-2 border rounded-md h-24" 
                placeholder="2023-10-01,1500.50,REF01,Abono cliente..."
                value={fileContent}
                onChange={e => setFileContent(e.target.value)}
              />
              <Button variant="secondary" className="w-full" onClick={handleImportar} disabled={!fileContent || importar.isPending}>
                <Upload className="mr-2 h-4 w-4" /> Importar Movimientos
              </Button>
            </CardContent>
          </Card>

          <Card className={conciliacionId ? "border-primary shadow-md" : "opacity-50 pointer-events-none"}>
            <CardHeader>
              <CardTitle className="text-lg flex items-center gap-2"><div className="bg-primary/10 p-2 rounded-full text-primary">3</div> Ejecutar</CardTitle>
              <CardDescription>Ejecuta el cruce algorítmico</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4 flex flex-col items-center justify-center pt-8">
              <div className="text-center mb-4 text-sm text-muted-foreground">
                El sistema buscará coincidencias exactas por monto y aproximadas por fecha/referencia.
              </div>
              <Button size="lg" className="w-full font-bold bg-indigo-600 hover:bg-indigo-700 text-white" onClick={handleCruzar} disabled={cruzar.isPending}>
                {cruzar.isPending ? <Loader2 className="mr-2 h-5 w-5 animate-spin" /> : <PlayCircle className="mr-2 h-5 w-5" />}
                Ejecutar Cruce
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>
    </AppLayout>
  );
}

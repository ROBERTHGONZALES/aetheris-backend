import { AppLayout } from "@/components/layout";
import { useGetPresupuestos, useCreatePresupuesto, useGetPresupuestosEnAlerta } from "@/hooks/use-presupuesto";
import { useGetSedes } from "@/hooks/use-sedes";
import { formatCurrency } from "@/lib/utils";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Progress } from "@/components/ui/progress";
import { useToast } from "@/hooks/use-toast";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Plus, Loader2, AlertTriangle } from "lucide-react";
import { useState, useEffect } from "react";
import { format } from "date-fns";

const createPresupuestoSchema = z.object({
  periodo: z.string().regex(/^\d{4}-\d{2}$/, "Formato YYYY-MM requerido"),
  montoPresupuestado: z.coerce.number().positive(),
  sede: z.object({ id: z.string().min(1, "Sede requerida") }),
  categoria: z.object({ id: z.string().min(1, "Categoría requerida") })
});

// El backend no expone un endpoint para listar categorías contables, así que
// no hay forma de descubrir sus IDs reales desde el frontend. Estos son los
// IDs sembrados que existen en la base de datos (verificados manualmente);
// si el backend llega a exponer GET /api/categorias, esto debería reemplazarse
// por esa lista real en vez de estar hardcodeado.
const CATEGORIAS_CONOCIDAS = [
  { id: "40000000-0000-0000-0000-000000000001", nombre: "Categoría 1" },
  { id: "40000000-0000-0000-0000-000000000002", nombre: "Categoría 2" },
  { id: "40000000-0000-0000-0000-000000000003", nombre: "Categoría 3" },
  { id: "40000000-0000-0000-0000-000000000004", nombre: "Categoría 4" },
  { id: "40000000-0000-0000-0000-000000000005", nombre: "Categoría 5" },
];

export default function Presupuesto() {
  const currentPeriod = format(new Date(), "yyyy-MM");
  const { data: sedes } = useGetSedes();
  const [selectedSedeId, setSelectedSedeId] = useState<string | undefined>(undefined);

  // Default to the first sede once sedes load, since GET /api/presupuesto requires sedeId.
  useEffect(() => {
    if (!selectedSedeId && sedes && sedes.length > 0) {
      setSelectedSedeId(sedes[0].id);
    }
  }, [sedes, selectedSedeId]);

  const { data: presupuestos, isLoading, isError, error } = useGetPresupuestos(selectedSedeId);
  const { data: enAlerta } = useGetPresupuestosEnAlerta();

  const createP = useCreatePresupuesto();
  const { toast } = useToast();

  const [openCreate, setOpenCreate] = useState(false);

  const form = useForm<z.infer<typeof createPresupuestoSchema>>({
    resolver: zodResolver(createPresupuestoSchema),
    defaultValues: {
      periodo: currentPeriod,
      montoPresupuestado: 0,
      sede: { id: "" },
      categoria: { id: "" }
    }
  });

  return (
    <AppLayout>
      <div className="flex flex-col gap-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold tracking-tight">Presupuesto</h1>
            <p className="text-muted-foreground mt-1">Control de partidas y ejecución financiera.</p>
          </div>
          
          <div className="flex items-center gap-4">
            <Select value={selectedSedeId} onValueChange={setSelectedSedeId}>
              <SelectTrigger className="w-48">
                <SelectValue placeholder="Selecciona una sede" />
              </SelectTrigger>
              <SelectContent>
                {sedes?.map((s) => (
                  <SelectItem key={s.id} value={s.id}>{s.nombre}</SelectItem>
                ))}
              </SelectContent>
            </Select>

            <Dialog open={openCreate} onOpenChange={setOpenCreate}>
              <DialogTrigger asChild>
                <Button><Plus className="mr-2 h-4 w-4" /> Asignar Presupuesto</Button>
              </DialogTrigger>
              <DialogContent>
                <DialogHeader>
                  <DialogTitle>Nueva Asignación Presupuestaria</DialogTitle>
                  <DialogDescription>Define el límite para una sede y categoría en el periodo.</DialogDescription>
                </DialogHeader>
                
                <Form {...form}>
                  <form onSubmit={form.handleSubmit(v => createP.mutate(v, {
                    onSuccess: () => { toast({title: "Asignado"}); setOpenCreate(false); form.reset(); },
                    onError: (err: any) => {
                      toast({
                        title: "No se pudo asignar el presupuesto",
                        description: err?.message ?? "Ocurrió un error inesperado. Intenta de nuevo.",
                        variant: "destructive",
                      });
                    }
                  }))} className="space-y-4">
                    <FormField
                      control={form.control}
                      name="periodo"
                      render={({ field }) => (
                        <FormItem><FormLabel>Periodo</FormLabel><FormControl><Input type="month" {...field} /></FormControl><FormMessage /></FormItem>
                      )}
                    />
                    <FormField
                      control={form.control}
                      name="montoPresupuestado"
                      render={({ field }) => (
                        <FormItem><FormLabel>Monto Límite</FormLabel><FormControl><Input type="number" {...field} /></FormControl><FormMessage /></FormItem>
                      )}
                    />
                    <FormField
                      control={form.control}
                      name="sede.id"
                      render={({ field }) => (
                        <FormItem><FormLabel>Sede</FormLabel>
                          <Select onValueChange={field.onChange} defaultValue={field.value}>
                            <FormControl><SelectTrigger><SelectValue placeholder="Selecciona..." /></SelectTrigger></FormControl>
                            <SelectContent>{sedes?.map(s => <SelectItem key={s.id} value={s.id}>{s.nombre}</SelectItem>)}</SelectContent>
                          </Select>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                    {/* El backend no expone un listado de categorías; usamos las IDs sembradas conocidas. */}
                    <FormField
                      control={form.control}
                      name="categoria.id"
                      render={({ field }) => (
                        <FormItem><FormLabel>Categoría</FormLabel>
                          <Select onValueChange={field.onChange} defaultValue={field.value}>
                            <FormControl><SelectTrigger><SelectValue placeholder="Selecciona..." /></SelectTrigger></FormControl>
                            <SelectContent>{CATEGORIAS_CONOCIDAS.map(c => <SelectItem key={c.id} value={c.id}>{c.nombre}</SelectItem>)}</SelectContent>
                          </Select>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                    <Button type="submit" className="w-full" disabled={createP.isPending}>
                      {createP.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />} Guardar
                    </Button>
                  </form>
                </Form>
              </DialogContent>
            </Dialog>
          </div>
        </div>

        <div className="bg-card border rounded-lg overflow-hidden shadow-sm">
          <Table>
            <TableHeader className="bg-muted/50">
              <TableRow>
                <TableHead>Categoría</TableHead>
                <TableHead>Periodo</TableHead>
                <TableHead className="text-right">Asignado</TableHead>
                <TableHead className="text-right">Ejecutado</TableHead>
                <TableHead>Estado de Ejecución</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {!selectedSedeId ? (
                <TableRow><TableCell colSpan={5} className="text-center py-8 text-muted-foreground">Selecciona una sede para ver su presupuesto.</TableCell></TableRow>
              ) : isLoading ? (
                <TableRow><TableCell colSpan={5} className="text-center py-8">Cargando...</TableCell></TableRow>
              ) : isError ? (
                <TableRow><TableCell colSpan={5} className="text-center py-8 text-destructive">Error al cargar el presupuesto: {(error as any)?.message ?? "intenta de nuevo."}</TableCell></TableRow>
              ) : !presupuestos || presupuestos.length === 0 ? (
                <TableRow><TableCell colSpan={5} className="text-center py-8">No hay partidas presupuestarias para esta sede.</TableCell></TableRow>
              ) : (
                presupuestos.map(p => (
                  <TableRow key={p.id}>
                    <TableCell className="font-medium">{p.categoria?.nombre || 'General'}</TableCell>
                    <TableCell className="font-mono text-sm">{p.periodo}</TableCell>
                    <TableCell className="text-right">{formatCurrency(p.montoPresupuestado)}</TableCell>
                    <TableCell className="text-right font-medium">{formatCurrency(p.montoEjecutado)}</TableCell>
                    <TableCell className="w-[30%]">
                      <div className="flex items-center gap-3">
                        <Progress 
                          value={p.porcentajeEjecucion} 
                          indicatorColor={p.porcentajeEjecucion > 90 ? "bg-red-500" : p.porcentajeEjecucion > 75 ? "bg-amber-500" : "bg-emerald-500"} 
                          className="h-2 flex-1" 
                        />
                        <span className="text-xs font-mono font-medium min-w-[40px] text-right">{Math.round(p.porcentajeEjecucion)}%</span>
                      </div>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </div>

        {enAlerta && enAlerta.length > 0 && (
          <div className="bg-amber-50 border border-amber-200 rounded-lg p-4 flex items-start gap-3">
            <AlertTriangle className="h-5 w-5 text-amber-600 mt-0.5 shrink-0" />
            <div>
              <p className="font-medium text-amber-900">{enAlerta.length} partida(s) en alerta por umbral de ejecución</p>
              <p className="text-sm text-amber-700">Estas partidas, en cualquier sede, superaron el umbral configurado de ejecución presupuestal.</p>
            </div>
          </div>
        )}
      </div>
    </AppLayout>
  );
}

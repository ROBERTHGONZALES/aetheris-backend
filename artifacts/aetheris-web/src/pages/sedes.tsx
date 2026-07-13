import { AppLayout } from "@/components/layout";
import { useGetSedes, useCreateSede, useUpdateLimiteSede } from "@/hooks/use-sedes";
import { formatCurrency } from "@/lib/utils";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { useToast } from "@/hooks/use-toast";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Plus, Loader2, Edit2 } from "lucide-react";
import { useState } from "react";

const createSedeSchema = z.object({
  nombre: z.string().min(2, "Nombre requerido"),
  codigo: z.string().min(2, "Código requerido").toUpperCase(),
  pais: z.string().min(2, "País requerido"),
  moneda: z.string().length(3, "Moneda 3 letras (ej. USD)"),
  montoLimiteAprobacion: z.coerce.number().positive("Debe ser mayor a 0")
});

export default function Sedes() {
  const { data: sedes, isLoading, isError, error } = useGetSedes();
  const createS = useCreateSede();
  const updateLimite = useUpdateLimiteSede();
  const { toast } = useToast();
  
  const [open, setOpen] = useState(false);
  const [editingLimit, setEditingLimit] = useState<{id: string, monto: string} | null>(null);

  const form = useForm<z.infer<typeof createSedeSchema>>({
    resolver: zodResolver(createSedeSchema),
    defaultValues: { nombre: "", codigo: "", pais: "", moneda: "USD", montoLimiteAprobacion: 5000 }
  });

  const onSubmit = (values: z.infer<typeof createSedeSchema>) => {
    createS.mutate(values, {
      onSuccess: () => {
        toast({ title: "Sede creada" });
        setOpen(false);
        form.reset();
      }
    });
  };

  const handleUpdateLimit = (id: string) => {
    if (!editingLimit || isNaN(Number(editingLimit.monto))) return;
    updateLimite.mutate({ id, monto: Number(editingLimit.monto) }, {
      onSuccess: () => {
        toast({ title: "Límite actualizado" });
        setEditingLimit(null);
      }
    });
  };

  return (
    <AppLayout>
      <div className="flex flex-col gap-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold tracking-tight">Sedes Operativas</h1>
            <p className="text-muted-foreground mt-1">Gestión de sucursales y límites de autonomía.</p>
          </div>
          
          <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>
              <Button><Plus className="mr-2 h-4 w-4" /> Registrar Sede</Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Nueva Sede Corporativa</DialogTitle>
              </DialogHeader>
              
              <Form {...form}>
                <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                  <FormField control={form.control} name="nombre" render={({ field }) => (
                    <FormItem><FormLabel>Nombre Comercial</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>
                  )} />
                  <div className="grid grid-cols-2 gap-4">
                    <FormField control={form.control} name="codigo" render={({ field }) => (
                      <FormItem><FormLabel>Código Interno</FormLabel><FormControl><Input {...field} placeholder="Ej. NY-01" /></FormControl><FormMessage /></FormItem>
                    )} />
                    <FormField control={form.control} name="pais" render={({ field }) => (
                      <FormItem><FormLabel>País</FormLabel><FormControl><Input {...field} /></FormControl><FormMessage /></FormItem>
                    )} />
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <FormField control={form.control} name="moneda" render={({ field }) => (
                      <FormItem><FormLabel>Moneda Base</FormLabel><FormControl><Input {...field} placeholder="USD" maxLength={3} /></FormControl><FormMessage /></FormItem>
                    )} />
                    <FormField control={form.control} name="montoLimiteAprobacion" render={({ field }) => (
                      <FormItem><FormLabel>Límite Autonomía</FormLabel><FormControl><Input type="number" {...field} /></FormControl><FormMessage /></FormItem>
                    )} />
                  </div>
                  <Button type="submit" className="w-full mt-2" disabled={createS.isPending}>
                    {createS.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />} Registrar
                  </Button>
                </form>
              </Form>
            </DialogContent>
          </Dialog>
        </div>

        <div className="bg-card border rounded-lg overflow-hidden shadow-sm">
          <Table>
            <TableHeader className="bg-muted/50">
              <TableRow>
                <TableHead>Código</TableHead>
                <TableHead>Nombre / País</TableHead>
                <TableHead>Estado</TableHead>
                <TableHead className="text-right">Moneda</TableHead>
                <TableHead className="text-right">Límite Aprobación Automática</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading ? (
                <TableRow><TableCell colSpan={5} className="text-center py-8">Cargando...</TableCell></TableRow>
              ) : isError ? (
                <TableRow><TableCell colSpan={5} className="text-center py-8 text-destructive">Error al cargar sedes: {(error as any)?.message ?? "intenta de nuevo."}</TableCell></TableRow>
              ) : !sedes || sedes.length === 0 ? (
                <TableRow><TableCell colSpan={5} className="text-center py-8">No hay sedes registradas.</TableCell></TableRow>
              ) : (
                sedes.map(s => (
                  <TableRow key={s.id}>
                    <TableCell className="font-mono font-medium">{s.codigo}</TableCell>
                    <TableCell>
                      <div className="font-medium">{s.nombre}</div>
                      <div className="text-xs text-muted-foreground">{s.pais}</div>
                    </TableCell>
                    <TableCell>
                      {s.estado ? (
                        <Badge variant="outline" className="bg-emerald-50 text-emerald-700 border-emerald-200">ACTIVA</Badge>
                      ) : (
                        <Badge variant="outline" className="bg-red-50 text-red-700 border-red-200">INACTIVA</Badge>
                      )}
                    </TableCell>
                    <TableCell className="text-right font-mono text-muted-foreground">{s.moneda}</TableCell>
                    <TableCell className="text-right">
                      {editingLimit?.id === s.id ? (
                        <div className="flex items-center justify-end gap-2">
                          <Input type="number" className="h-8 w-24 text-right" value={editingLimit.monto} onChange={e => setEditingLimit({...editingLimit, monto: e.target.value})} />
                          <Button size="sm" onClick={() => handleUpdateLimit(s.id)} disabled={updateLimite.isPending}>OK</Button>
                          <Button size="sm" variant="ghost" onClick={() => setEditingLimit(null)}>X</Button>
                        </div>
                      ) : (
                        <div className="flex items-center justify-end gap-2 group cursor-pointer" onClick={() => setEditingLimit({id: s.id, monto: s.montoLimiteAprobacion.toString()})}>
                          <span className="font-medium">{formatCurrency(s.montoLimiteAprobacion, s.moneda)}</span>
                          <Edit2 className="h-3 w-3 text-muted-foreground opacity-0 group-hover:opacity-100 transition-opacity" />
                        </div>
                      )}
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </div>
      </div>
    </AppLayout>
  );
}

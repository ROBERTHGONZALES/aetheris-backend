import { AppLayout } from "@/components/layout";
import { useGetTransacciones, useCreateTransaccion } from "@/hooks/use-transacciones";
import { useGetSedes } from "@/hooks/use-sedes";
import { formatCurrency, formatDate } from "@/lib/utils";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { useToast } from "@/hooks/use-toast";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Plus, Loader2 } from "lucide-react";
import { useState } from "react";

const createTxSchema = z.object({
  tipo: z.enum(["INGRESO", "EGRESO"]),
  monto: z.coerce.number().positive("El monto debe ser positivo"),
  moneda: z.string().min(3),
  fecha: z.string().min(1, "Fecha requerida"),
  descripcion: z.string().min(5, "Descripción muy corta"),
  sede: z.object({ id: z.string() }),
  categoria: z.object({ id: z.string().optional() })
});

export default function Transacciones() {
  const { data: transacciones, isLoading, isError, error } = useGetTransacciones();
  const { data: sedes } = useGetSedes();
  const createTx = useCreateTransaccion();
  const { toast } = useToast();
  const [open, setOpen] = useState(false);

  const form = useForm<z.infer<typeof createTxSchema>>({
    resolver: zodResolver(createTxSchema),
    defaultValues: {
      tipo: "INGRESO",
      monto: 0,
      moneda: "USD",
      fecha: new Date().toISOString().split('T')[0],
      descripcion: "",
      sede: { id: "" },
      categoria: { id: "" }
    }
  });

  function onSubmit(values: z.infer<typeof createTxSchema>) {
    createTx.mutate(values, {
      onSuccess: () => {
        toast({ title: "Transacción creada" });
        setOpen(false);
        form.reset();
      },
      onError: (err: any) => {
        toast({ variant: "destructive", title: "Error", description: err.message });
      }
    });
  }

  return (
    <AppLayout>
      <div className="flex flex-col gap-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold tracking-tight">Transacciones</h1>
            <p className="text-muted-foreground mt-1">Registro central de ingresos y egresos.</p>
          </div>
          
          <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>
              <Button><Plus className="mr-2 h-4 w-4" /> Nueva Transacción</Button>
            </DialogTrigger>
            <DialogContent className="sm:max-w-[500px]">
              <DialogHeader>
                <DialogTitle>Registrar Transacción</DialogTitle>
                <DialogDescription>
                  Ingresa los detalles de la operación financiera.
                </DialogDescription>
              </DialogHeader>
              
              <Form {...form}>
                <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <FormField
                      control={form.control}
                      name="tipo"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Tipo</FormLabel>
                          <Select onValueChange={field.onChange} defaultValue={field.value}>
                            <FormControl>
                              <SelectTrigger>
                                <SelectValue placeholder="Selecciona" />
                              </SelectTrigger>
                            </FormControl>
                            <SelectContent>
                              <SelectItem value="INGRESO">Ingreso</SelectItem>
                              <SelectItem value="EGRESO">Egreso</SelectItem>
                            </SelectContent>
                          </Select>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                    <FormField
                      control={form.control}
                      name="fecha"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Fecha</FormLabel>
                          <FormControl>
                            <Input type="date" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <FormField
                      control={form.control}
                      name="monto"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Monto</FormLabel>
                          <FormControl>
                            <Input type="number" step="0.01" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                    <FormField
                      control={form.control}
                      name="moneda"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Moneda</FormLabel>
                          <FormControl>
                            <Input {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </div>

                  <FormField
                    control={form.control}
                    name="sede.id"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Sede</FormLabel>
                        <Select onValueChange={field.onChange} defaultValue={field.value}>
                          <FormControl>
                            <SelectTrigger>
                              <SelectValue placeholder="Selecciona una sede" />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            {sedes?.map(s => (
                              <SelectItem key={s.id} value={s.id}>{s.nombre}</SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  <FormField
                    control={form.control}
                    name="descripcion"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Descripción</FormLabel>
                        <FormControl>
                          <Input {...field} placeholder="Motivo de la transacción" />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  <Button type="submit" className="w-full mt-4" disabled={createTx.isPending}>
                    {createTx.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                    Guardar Transacción
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
                <TableHead>Fecha</TableHead>
                <TableHead>Descripción</TableHead>
                <TableHead>Sede</TableHead>
                <TableHead>Tipo</TableHead>
                <TableHead className="text-right">Monto</TableHead>
                <TableHead className="text-center">Estado</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-center py-8 text-muted-foreground">Cargando transacciones...</TableCell>
                </TableRow>
              ) : isError ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-center py-8 text-destructive">Error al cargar transacciones: {(error as any)?.message ?? "intenta de nuevo."}</TableCell>
                </TableRow>
              ) : !transacciones || transacciones.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-center py-8 text-muted-foreground">No hay registros.</TableCell>
                </TableRow>
              ) : (
                transacciones.map(t => (
                  <TableRow key={t.id}>
                    <TableCell className="font-medium">{formatDate(t.fecha)}</TableCell>
                    <TableCell>{t.descripcion}</TableCell>
                    <TableCell>{t.sede?.nombre}</TableCell>
                    <TableCell>
                      <span className={`text-xs font-bold ${t.tipo === 'INGRESO' ? 'text-emerald-600' : 'text-red-600'}`}>
                        {t.tipo}
                      </span>
                    </TableCell>
                    <TableCell className="text-right font-mono font-medium">
                      {formatCurrency(t.monto, t.moneda)}
                    </TableCell>
                    <TableCell className="text-center">
                      <Badge variant={
                        t.estadoAprobacion === 'APROBADA' ? 'success' : 
                        t.estadoAprobacion === 'RECHAZADA' ? 'destructive' : 'warning'
                      }>
                        {t.estadoAprobacion}
                      </Badge>
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

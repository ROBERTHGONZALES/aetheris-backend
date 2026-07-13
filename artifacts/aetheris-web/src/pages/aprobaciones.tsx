import { AppLayout } from "@/components/layout";
import { useGetAprobacionesPendientes, useAprobarTransaccion, useRechazarTransaccion } from "@/hooks/use-aprobaciones";
import { formatCurrency, formatDate } from "@/lib/utils";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Textarea } from "@/components/ui/textarea";
import { useToast } from "@/hooks/use-toast";
import { useState } from "react";
import { Check, X, Loader2, AlertCircle } from "lucide-react";
import { Alert, AlertDescription } from "@/components/ui/alert";

export default function Aprobaciones() {
  const { data: pendientes, isLoading, isError, error } = useGetAprobacionesPendientes();
  const aprobar = useAprobarTransaccion();
  const rechazar = useRechazarTransaccion();
  const { toast } = useToast();
  
  const [selectedTx, setSelectedTx] = useState<any>(null);
  const [observacion, setObservacion] = useState("");
  const [actionType, setActionType] = useState<"APROBAR" | "RECHAZAR" | null>(null);

  const handleAction = () => {
    if (!selectedTx || !actionType) return;
    
    const mutation = actionType === "APROBAR" ? aprobar : rechazar;
    
    mutation.mutate({ id: selectedTx.id, observacion }, {
      onSuccess: () => {
        toast({ title: `Transacción ${actionType.toLowerCase()}da exitosamente.` });
        setSelectedTx(null);
        setObservacion("");
        setActionType(null);
      },
      onError: (err: any) => {
        toast({ variant: "destructive", title: "Error", description: err.message });
      }
    });
  };

  const isPending = aprobar.isPending || rechazar.isPending;

  return (
    <AppLayout>
      <div className="flex flex-col gap-6">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Aprobaciones Pendientes</h1>
          <p className="text-muted-foreground mt-1">Revisión de transacciones que superan el límite de sede.</p>
        </div>

        <div className="bg-card border rounded-lg overflow-hidden shadow-sm">
          <Table>
            <TableHeader className="bg-muted/50">
              <TableRow>
                <TableHead>Fecha Solicitud</TableHead>
                <TableHead>Sede</TableHead>
                <TableHead>Descripción</TableHead>
                <TableHead className="text-right">Monto Límite</TableHead>
                <TableHead className="text-right">Monto Solicitado</TableHead>
                <TableHead className="text-right">Acciones</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-center py-8 text-muted-foreground">Cargando pendientes...</TableCell>
                </TableRow>
              ) : isError ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-center py-8 text-destructive">Error al cargar aprobaciones: {(error as any)?.message ?? "intenta de nuevo."}</TableCell>
                </TableRow>
              ) : !pendientes || pendientes.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-center py-12">
                    <div className="flex flex-col items-center justify-center text-muted-foreground">
                      <Check className="h-8 w-8 mb-2 opacity-50" />
                      <p>No hay aprobaciones pendientes en este momento.</p>
                    </div>
                  </TableCell>
                </TableRow>
              ) : (
                pendientes.map(p => (
                  <TableRow key={p.id}>
                    <TableCell className="font-medium">{formatDate(p.fechaSolicitud)}</TableCell>
                    <TableCell>{p.transaccion.sede.nombre}</TableCell>
                    <TableCell>{p.transaccion.descripcion}</TableCell>
                    <TableCell className="text-right text-muted-foreground">{formatCurrency(p.montoLimite)}</TableCell>
                    <TableCell className="text-right font-bold font-mono">{formatCurrency(p.transaccion.monto)}</TableCell>
                    <TableCell className="text-right space-x-2">
                      <Button size="sm" variant="outline" className="text-emerald-600 border-emerald-200 hover:bg-emerald-50 dark:hover:bg-emerald-900/20" onClick={() => { setSelectedTx(p); setActionType("APROBAR"); }}>
                        <Check className="h-4 w-4 mr-1" /> Aprobar
                      </Button>
                      <Button size="sm" variant="outline" className="text-red-600 border-red-200 hover:bg-red-50 dark:hover:bg-red-900/20" onClick={() => { setSelectedTx(p); setActionType("RECHAZAR"); }}>
                        <X className="h-4 w-4 mr-1" /> Rechazar
                      </Button>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </div>

        <Dialog open={!!selectedTx} onOpenChange={(open) => !open && setSelectedTx(null)}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>{actionType === "APROBAR" ? "Aprobar Transacción" : "Rechazar Transacción"}</DialogTitle>
              <DialogDescription>
                {selectedTx && `Confirmar acción para la transacción "${selectedTx.transaccion.descripcion}" por ${formatCurrency(selectedTx.transaccion.monto)}.`}
              </DialogDescription>
            </DialogHeader>

            {actionType === "APROBAR" && (
              <Alert className="bg-blue-50 text-blue-800 border-blue-200 dark:bg-blue-900/20 dark:border-blue-800/50 dark:text-blue-300">
                <AlertCircle className="h-4 w-4" />
                <AlertDescription>
                  Esta acción autorizará el flujo de fondos. Quedará registrado en auditoría.
                </AlertDescription>
              </Alert>
            )}

            <div className="space-y-4 py-4">
              <div className="space-y-2">
                <label className="text-sm font-medium">Observaciones (opcional)</label>
                <Textarea 
                  placeholder="Añade un comentario sobre tu decisión..."
                  value={observacion}
                  onChange={(e) => setObservacion(e.target.value)}
                />
              </div>
            </div>

            <div className="flex justify-end gap-3">
              <Button variant="outline" onClick={() => setSelectedTx(null)} disabled={isPending}>Cancelar</Button>
              <Button 
                variant={actionType === "APROBAR" ? "default" : "destructive"} 
                onClick={handleAction}
                disabled={isPending}
              >
                {isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                Confirmar
              </Button>
            </div>
          </DialogContent>
        </Dialog>

      </div>
    </AppLayout>
  );
}

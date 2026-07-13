import { AppLayout } from "@/components/layout";
import { useGetAuditoria } from "@/hooks/use-auditoria";
import { formatDate } from "@/lib/utils";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { ShieldAlert, Monitor } from "lucide-react";

const CATEGORIA_COLORS: Record<string, string> = {
  SESION:       "bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-300",
  TRANSACCION:  "bg-green-100 text-green-800 dark:bg-green-900/40 dark:text-green-300",
  FLUJO_APROBACION: "bg-orange-100 text-orange-800 dark:bg-orange-900/40 dark:text-orange-300",
  PRESUPUESTO:  "bg-purple-100 text-purple-800 dark:bg-purple-900/40 dark:text-purple-300",
  SEDE:         "bg-yellow-100 text-yellow-800 dark:bg-yellow-900/40 dark:text-yellow-300",
};

function accionColor(categoria: string | null) {
  return CATEGORIA_COLORS[categoria ?? ""] ?? "bg-slate-100 text-slate-800 dark:bg-slate-800 dark:text-slate-300";
}

export default function Auditoria() {
  const { data: logs, isLoading, isError, error } = useGetAuditoria();

  const colSpan = 6;

  return (
    <AppLayout>
      <div className="flex flex-col gap-6">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Registro de Auditoría</h1>
          <p className="text-muted-foreground mt-1">Trazabilidad inmutable de acciones críticas en el sistema.</p>
        </div>

        <Card className="border-border/50 shadow-sm">
          <CardHeader className="bg-muted/30 border-b">
            <CardTitle className="text-base flex items-center gap-2">
              <ShieldAlert className="h-5 w-5 text-primary" />
              Eventos del Sistema
              {logs && (
                <Badge variant="secondary" className="ml-auto font-normal">
                  {logs.length} registros
                </Badge>
              )}
            </CardTitle>
          </CardHeader>
          <CardContent className="p-0">
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-[160px] whitespace-nowrap">Fecha</TableHead>
                    <TableHead className="min-w-[160px]">Usuario</TableHead>
                    <TableHead className="w-[120px]">Módulo</TableHead>
                    <TableHead className="min-w-[180px]">Acción</TableHead>
                    <TableHead className="min-w-[140px]">Entidad Afectada</TableHead>
                    <TableHead className="w-[130px]">IP</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {isLoading ? (
                    <TableRow>
                      <TableCell colSpan={colSpan} className="text-center py-8 text-muted-foreground">
                        Cargando logs...
                      </TableCell>
                    </TableRow>
                  ) : isError ? (
                    <TableRow>
                      <TableCell colSpan={colSpan} className="text-center py-8 text-destructive">
                        Error al cargar auditoría: {(error as any)?.message ?? "intenta de nuevo."}
                      </TableCell>
                    </TableRow>
                  ) : !logs || logs.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={colSpan} className="text-center py-8 text-muted-foreground">
                        No hay registros de auditoría.
                      </TableCell>
                    </TableRow>
                  ) : (
                    logs.map((l, idx) => (
                      <TableRow key={l.id ?? idx} className="hover:bg-muted/30">

                        {/* Fecha */}
                        <TableCell className="font-mono text-xs text-muted-foreground whitespace-nowrap">
                          {formatDate(l.fechaHora, true)}
                        </TableCell>

                        {/* Usuario */}
                        <TableCell>
                          {l.usuario ? (
                            <div className="flex flex-col">
                              <span className="font-medium text-sm">{l.usuario}</span>
                              {l.correoUsuario && (
                                <span className="text-xs text-muted-foreground">{l.correoUsuario}</span>
                              )}
                            </div>
                          ) : (
                            <span className="inline-flex items-center gap-1 text-xs text-muted-foreground italic">
                              <Monitor className="h-3 w-3" />
                              Sistema
                            </span>
                          )}
                        </TableCell>

                        {/* Módulo */}
                        <TableCell>
                          <span className="font-mono text-xs text-muted-foreground">
                            {l.modulo ?? "—"}
                          </span>
                        </TableCell>

                        {/* Acción */}
                        <TableCell>
                          <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${accionColor(l.categoria)}`}>
                            {l.accion}
                          </span>
                        </TableCell>

                        {/* Entidad Afectada */}
                        <TableCell className="font-mono text-xs text-muted-foreground">
                          {l.entidadAfectada ?? "—"}
                        </TableCell>

                        {/* IP */}
                        <TableCell className="font-mono text-xs text-muted-foreground">
                          {l.direccionIp ?? "—"}
                        </TableCell>

                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </div>
          </CardContent>
        </Card>
      </div>
    </AppLayout>
  );
}

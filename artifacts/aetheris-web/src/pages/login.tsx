import { useState } from "react";
import { useLocation } from "wouter";
import { z } from "zod";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useLogin, useUser } from "@/hooks/use-auth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { useToast } from "@/hooks/use-toast";
import { Loader2 } from "lucide-react";

const loginSchema = z.object({
  correo: z.string().email({ message: "Correo inválido" }),
  password: z.string().min(4, { message: "Contraseña requerida" })
});

export default function Login() {
  const [, setLocation] = useLocation();
  const { toast } = useToast();
  const login = useLogin();
  const user = useUser();

  if (user) {
    setLocation("/dashboard");
    return null;
  }

  const form = useForm<z.infer<typeof loginSchema>>({
    resolver: zodResolver(loginSchema),
    defaultValues: { correo: "", password: "" }
  });

  function onSubmit(values: z.infer<typeof loginSchema>) {
    login.mutate(values, {
      onSuccess: () => {
        toast({ title: "Acceso concedido", description: "Bienvenido a AETHERIS." });
        setLocation("/dashboard");
      },
      onError: (error: any) => {
        toast({ 
          variant: "destructive", 
          title: "Acceso denegado", 
          description: error.message || "Credenciales incorrectas" 
        });
      }
    });
  }

  return (
    <div className="min-h-screen w-full flex items-center justify-center bg-background relative overflow-hidden">
      {/* Decorative background elements for a "corporate precise" feel */}
      <div className="absolute top-0 left-0 w-full h-full pointer-events-none opacity-[0.03]" 
           style={{ backgroundImage: 'linear-gradient(rgba(255, 255, 255, .2) 1px, transparent 1px), linear-gradient(90deg, rgba(255, 255, 255, .2) 1px, transparent 1px)', backgroundSize: '40px 40px' }} />
      <div className="absolute top-[-20%] left-[-10%] w-[50%] h-[50%] bg-primary/5 blur-[120px] rounded-full" />
      <div className="absolute bottom-[-20%] right-[-10%] w-[50%] h-[50%] bg-primary/5 blur-[120px] rounded-full" />

      <Card className="w-full max-w-md mx-4 shadow-2xl border-border/50 backdrop-blur-sm z-10">
        <CardHeader className="space-y-3 pb-8 text-center pt-10">
          <div className="mx-auto h-12 w-12 rounded-xl bg-primary flex items-center justify-center mb-2 shadow-inner">
            <span className="text-primary-foreground font-bold text-xl tracking-tighter">AE</span>
          </div>
          <CardTitle className="text-2xl font-bold tracking-tight">AETHERIS</CardTitle>
          <CardDescription className="text-sm tracking-wide uppercase text-muted-foreground font-medium">
            Portal de Control Financiero
          </CardDescription>
        </CardHeader>
        <CardContent className="pb-10 px-8">
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
              <FormField
                control={form.control}
                name="correo"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="text-xs uppercase tracking-wider text-muted-foreground font-semibold">Correo Corporativo</FormLabel>
                    <FormControl>
                      <Input placeholder="usuario@empresa.com" className="h-11 bg-muted/30" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="password"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="text-xs uppercase tracking-wider text-muted-foreground font-semibold">Contraseña</FormLabel>
                    <FormControl>
                      <Input type="password" placeholder="••••••••" className="h-11 bg-muted/30" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <Button type="submit" className="w-full h-11 text-md font-medium mt-4" disabled={login.isPending}>
                {login.isPending ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
                Ingresar al Sistema
              </Button>
            </form>
          </Form>
        </CardContent>
      </Card>
    </div>
  );
}

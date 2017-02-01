/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Control;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import javax.jms.Session;

/**
 *
 * @author Mauricio
 */
public class RegistroHistorial {
    
    /**
    *
    * Convierte los milisegundos en dias, horas, minutos, segundos.
    *
    * @param tiempo en milisegundos que se desea convertir.
    * @return String que representa el tiempo convertido en dias, horas y minutos con el 
    * siguente formato #d #h #m.
    */
    private String convertidorDeTiempo(long tiempo){
        String tiempoConvertido = "";
        
        int auxseg = (int) tiempo / 1000;
        int dias = auxseg / 86400;
        int horas = (auxseg%86400) / 3600;
        int minutos = ((auxseg%86400)%3600) / 60;
        int segundos = ((auxseg%86400)%3600) % 60;
        
        tiempoConvertido = dias + "d " + horas + "h " + minutos + "m ";
        
        return tiempoConvertido;
    }

    /**
     * Obtiene una lista de viajes que realizo una unidad en un dia.
     *
     * @param flota Flota a la que pertence la unidad directamente.
     * @param unitno Identificador de la unidad en la base de datos.
     * @return Lista de números de viaje.
     */
    public List<Long> obtenerViajesDeUnidad(final String flota, final String unitno) {
        List<Long> lViajes = new ArrayList<Long>();
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        
        try {
            for (Object o : lViajes) {
                lViajes.add(Long.parseLong(o.toString()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lViajes;
    }

    /**
     * Obtine los valores que se toman para el resumen del viaje una unidad.
     *
     * @param idViajeSistema Identificador del viaje realizado por la unidad.
     * @param unitno Identificador de la unidad en la base de datos.
     * @param flota Nombre de la flota a la cual pertenece la unidad
     * directamente.
     * @return Lista de valores del resumen del viaje.
     */
    public Resumen obtenerResumen(final Long idViajeSistema, final String unitno, final String flota) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        DecimalFormat df = new DecimalFormat("###,###,###.00");
        Resumen r = new Resumen();
        try {
            Session s = null;
            SessionFactory sf = HibernateUtil2.getSessionFactory();
            s = sf.getCurrentSession();

            HibernateSentenceTransaction transaction = new HibernateSentenceTransaction(sf) {
                @Override
                public Object transaction(BasicHibernateSupport bhs, Object... os) throws HibernateException, Exception {
                    String consultaSql = "SELECT VH.alias, DV.placas, VS.fecha_salida,VS.fecha_entrada, \n"
                            + "       VS.kilometros, VS.combustible, O.nombre || ' '|| O.apellidos as operador \n"
                            + " FROM viaje_sistema  AS VS	\n"
                            + "	INNER JOIN vehiculos AS VH ON VS.unitno = VH.unitno \n"
                            + "	INNER JOIN documentos_vehiculo AS DV ON VS.unitno = DV.unitno \n"
                            + "	INNER JOIN operador AS O ON O.idoperador=VS.operador \n"
                            + " WHERE idviajesis = '" + idViajeSistema + "' \n"
                            + " AND VS.flota = '" + flota + "'\n"
                            + " AND VS.unitno = '" + unitno + "'"
                            + " AND VS.fecha_entrada is not null";

                    List l_resumen = bhs.getByNativeSQLQuery(consultaSql);
                    return l_resumen;
                }
            };
            List<Object[]> list = (List<Object[]>) transaction.execute();

            for (Object[] o : list) {
                r.setFolio_viaje(idViajeSistema);
                r.setTransportista(flota);
                r.setUniad(o[0].toString());
                r.setPlaca(o[1].toString());
                r.setInicio_viaje(dateFormat.format((Date) o[2]));

                //Si el viaje aún no ha terminado, se muestra texto de validación
                String fin_viaje;
                if (o[3] == null) {
                    fin_viaje = "En viaje";
                } else {
                    fin_viaje = dateFormat.format((Date) o[3]);
                }
                r.setFin_viaje(fin_viaje);

                //Calculando tiempo de viaje
                long resta = 0;
                if (o[3] == null) {
                    r.setTiempo_total_viaje("- - -");
                } else {
                    resta = ((Date) o[3]).getTime() - ((Date) o[2]).getTime();
                    r.setTiempo_total_viaje(convertidorDeTiempo(resta));
                }

                Double dViaje = (Double) o[4];
                r.setKilometro_total_viaje(df.format(dViaje) + " km");

                Double dCombustible = (Double) o[5];
                r.setCombustible_total_viaje(df.format(dCombustible) + " l");

                r.setRendimiento_viaje(dViaje / dCombustible);

                r.setCo2(df.format(dCombustible * 2.7676) + " kg");

                r.setOperador(o[6].toString());
            }
        } 
        catch (Exception e) { e.printStackTrace(); }
        
        return r;
    }

    /**
     * Obtiene la lista de alertas generadas durante el viaje de la unidad.
     *
     * @param unitno Identificador de la unidad en la base de datos.
     * @param flota Flota a la que pertenece la unidad directamente.
     * @param idViajeSistema Identificador del viaje realizado por la unidad.
     * @return Lista de alertas.
     */
    public List<AlertaDeOperacion> obtenerAlertas(final String unitno, final String flota, final Long idViajeSistema) 
    {
        List<AlertaDeOperacion> listaAlerta = new ArrayList<AlertaDeOperacion>();

        try 
        {
            Session s = null;
            SessionFactory sf = HibernateUtil2.getSessionFactory();
            s = sf.getCurrentSession();

            HibernateSentenceTransaction transaction = new HibernateSentenceTransaction(sf) {
                @Override
                public Object transaction(BasicHibernateSupport bhs, Object... os) throws HibernateException, Exception {

                    String consultaSql = "select EA.tipo, count(*), TE.nombre  from entradaalarma as EA\n"
                            + "INNER JOIN tipoevento as TE on EA.tipo = TE.tipo\n"
                            + "where idviajesis_ = '"+idViajeSistema+"' \n"
                            + "and EA.flota_ = '"+flota+"'\n"
                            + "and EA.unitno_ = '"+unitno+"'\n"
                            + "group by EA.tipo, TE.nombre";
                    List l_alertas = bhs.getByNativeSQLQuery(consultaSql);
                    return l_alertas;
                }
            };

            List<Object[]> list = (List<Object[]>) transaction.execute();

            for (Object[] o : list) {
                AlertaDeOperacion alertaDeOperacion = new AlertaDeOperacion();
                alertaDeOperacion.setNo_alertas(Long.parseLong(o[1].toString()));

                alertaDeOperacion.setTipo_alerta(o[2].toString());


                listaAlerta.add(alertaDeOperacion);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return listaAlerta;
    }

    /**
     * Obtiene los puntos de interés, de tipo <strong>cliente</strong>, donde se
     * detuvo la unidad durante un viaje especificado, asi como el tiempo
     * estimado de permanencia en el punto de interes.
     *
     * @param unitno Identificador de la unidad en la base de datos.
     * @param flota Nombre de la flota a la cual pertenece la unidad
     * directamente.
     * @param idViaje Identificador del viaje realizado por la unidad.
     * @return Lista de estancias en puntos de interes
     */
     public List<EstanciaEnCliente> obtenerEstanciaEnClientes(final String unitno, final String flota, final Long idViaje) {
        EstanciaEnCliente estanciaEnCliente;
        List<EstanciaEnCliente> listaCliente = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        try 
        {
            Session s = null;
            SessionFactory sf = HibernateUtil2.getSessionFactory();
            s = sf.getCurrentSession();

            HibernateSentenceTransaction transaction = new HibernateSentenceTransaction(sf) 
            {
                @Override
                public Object transaction(BasicHibernateSupport bhs, Object... os) throws HibernateException, Exception 
                {
                    String consultaSql = 
                      "SELECT punto, PI.nombre, fecha, status \n"
                    + "FROM puntosviaje AS PV \n"
                    + "INNER JOIN puntosdeinteres as PI ON PV.punto = PI.puntoid\n"
                    + "INNER JOIN categoriapunto as CP on PI.categoriaid = CP.categoriaid\n"
                    + "WHERE idviajesis = '" + idViaje + "' \n"
                    + "AND CP.descripcion = 'Cliente'\n"
                    + "AND (status='Entrando' OR  status = 'Saliendo')\n"
                    + "order by fecha"
                    ;
        
                    List lista = bhs.getByNativeSQLQuery(consultaSql);
                    return lista;
                }
            };
            
            List<Object[]> list = (List<Object[]>) transaction.execute();
            
            //Creación de dos listas para guardar las posiciones del punto y de la fecha de entrada
            //c: guarda el punto y d: guarda la fecha de llegada
            List<String> c = new ArrayList<String>();
            List<Date> d = new ArrayList<Date>();
            long resta = 0;
            for (Object[] o : list) 
            {
                estanciaEnCliente = new EstanciaEnCliente();
                
                //Si es "entrando" se guarda la fecha de entrada de acuerdo al punto correspondiente
                if(o[3].toString().equalsIgnoreCase("entrando"))
                {
                    estanciaEnCliente.setPunto(o[0].toString());
                    c.add(o[0].toString());
                    d.add((Date)o[2]);
                }
                
                //Se busca en la lista la anterior ocurrencia de el punto y se calcula, si es el caso,
                //el teimpo de estancia de la unidad en el punto
                //Se añade un elemento a la lista solo cuando se registra una fecha de salida
                if(o[3].toString().equalsIgnoreCase("saliendo"))
                {
                    int u=c.lastIndexOf(o[0].toString());
                    if(u>=0)
                    {
                        //Se calcula el tiempo en estancia.
                        resta = ((Date)o[2]).getTime()-d.get(u).getTime();
                       
                        estanciaEnCliente.setTiempo_estancia(convertidorDeTiempo(resta)); 
                        estanciaEnCliente.setFecha_llegada(dateFormat.format(d.get(u)));
                    }    
                    //si no existe una fecha de llegada a ese punto no se calcula el tiempo de estancia y no
                    //se muestra una llegada
                     else
                     {
                         estanciaEnCliente.setFecha_llegada("-----");
                         estanciaEnCliente.setTiempo_estancia("-----");
                     }
                     
                     estanciaEnCliente.setFecha_salida(dateFormat.format((Date)o[2]));
                     estanciaEnCliente.setNombre(o[1].toString());

                    listaCliente.add(estanciaEnCliente);
                }
            }
        }
        catch (Exception e) { e.printStackTrace(); }

        /**
         * SELECT punto, PI.nombre, fecha, status FROM
         * puntosviaje AS PV INNER JOIN puntosdeinteres as PI ON PV.punto =
         * PI.puntoid INNER JOIN categoriapunto as CP on PI.categoriaid =
         * CP.categoriaid WHERE idviajesis = '1528873' AND CP.descripcion =
         * 'Cliente' AND (status='Entrando' OR status = 'Saliendo') order by
         * fecha
         */
        
        return listaCliente;
    }

    /**
     * Muestra una lista de las alertas de detenido que tuvo la undiad durante
     * el viaje, indicando la ubicación geográfica donde se detuvo la unidad. Se
     * agrega el identificador de un detenido en punto negro.
     *
     * @param unitno Identificador del viaje realizado por la unidad.
     * @param flota Nombre de la flota a la cual pertenece la unidad
     * directamente.
     * @param idViaje Identificador del viaje realizado por la unidad.
     * @return Objeto de tipo DetenidoViaje que contiene una lista de los lugares y punto negro donde estuvo la unidad.
     */
    public DetenidoEnViaje obtenerDetenidoEnViaje(final String unitno, final String flota, final Long idViaje) {      
        DetenidoEnViaje detenidoEnViaje = new DetenidoEnViaje();
        List<RegistroDetenidoEnViaje> listaDetenidoEnViaje = new ArrayList<RegistroDetenidoEnViaje>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        try {
            Session s = null;
            SessionFactory sf = HibernateUtil2.getSessionFactory();
            s = sf.getCurrentSession();

            HibernateSentenceTransaction transaction = new HibernateSentenceTransaction(sf) {
                @Override
                public Object transaction(BasicHibernateSupport bhs, Object... os) throws HibernateException, Exception {

                    String consultaSql = "SELECT DEH.fechaentrada, EA.tipo, TE.nombre, EA.valor, TE.descrip, (cerca_de_ciudad_orientacion_punto_interes_tipo_2(DEH.lng,DEH.lat,1, VEH.subflota, VEH.flota))\n" +
                                        "FROM  entradaalarma as EA \n" +
                                        "INNER JOIN tipoevento as TE on EA.tipo = TE.tipo\n" +
                                        "INNER JOIN datosentrada_his AS DEH on DEH.importacionid = EA.importacionid \n" +
                                        "INNER JOIN vehiculos as VEH on DEH.unitno = VEH.unitno\n" +
                                        "WHERE DEH.flota= '" +flota+ "' \n" +
                                        "AND DEH.viajeactivo = '"+ idViaje +"' \n" +
                                        "AND DEH.unitno = '"+ unitno +"' \n" +
                                        "AND (EA.tipo=6 OR EA.tipo=7 OR EA.tipo=24 OR EA.tipo=25)" +
                    //-- 6=detenido y apagado, 7=detenido y encendido, 24=detendo, apagado en punto negro, 25=detenido, prendidoen punto negro \n" +
                                        "ORDER BY DEH.fechaentrada";
                    
                    List l_detenidoEnViaje = bhs.getByNativeSQLQuery(consultaSql);
                    return l_detenidoEnViaje;
                }
            };
            List<Object[]> list = (List<Object[]>) transaction.execute();  
            float total_tiempo = 0; // Total del Tiempo de la unidad detenida
            for (Object[] o : list) {
                RegistroDetenidoEnViaje registroDetenidoEnViaje = new RegistroDetenidoEnViaje();
                
                //Se obtiene la fecha de tipo Timestamp y de scomvierte a Date y se da formato
                Date fecha = new Date(((java.sql.Timestamp)o[0]).getTime());
                registroDetenidoEnViaje.setFecha(dateFormat.format(fecha));
                
                //Se obtiene la hora en formato decimal y se convierte en milisegundos
                double tiempo = Double.parseDouble(((String)o[3]).split(" ")[2]);
                long milisegundos = 0L;
                milisegundos += (int)tiempo * 3600000;
                milisegundos += (long) (((tiempo - (int)tiempo) * 60) * 60000);                
                
                total_tiempo += tiempo; //Se va sumando cada tiempo detenido para obtener el total

                registroDetenidoEnViaje.setTiempo_detenido(convertidorDeTiempo(milisegundos));

                //Se verifica que los datos para la columna referencia contengan datos
                //en caso de que la consulta no contenga datos se muestra el texto "Sin datos".
                if ( (String)o[4] == null ){
                  if( (String)o[5] == null ){
                      registroDetenidoEnViaje.setReferencia("Sin datos");
                  }else{
                      registroDetenidoEnViaje.setReferencia((String)o[5]);
                  }                     
                }else{
                  if( (String)o[5] == null ){
                     registroDetenidoEnViaje.setReferencia((String)o[4] + "." ); 
                  }else{
                     registroDetenidoEnViaje.setReferencia((String)o[4] + ". "+ (String)o[5]);
                  }
                }
                
                //Se valida, de acuerdo al "tipo", si el lugar donde se detuvo la unidad es punto negro.
                if( (int)o[1] == 24 || (int)o[1] == 25 ){
                    registroDetenidoEnViaje.setEs_punto_negro("Si");
                }else{
                    registroDetenidoEnViaje.setEs_punto_negro("No");
                }
    
                listaDetenidoEnViaje.add(registroDetenidoEnViaje);
            }
                   
            detenidoEnViaje.setTotal_detenidos(listaDetenidoEnViaje.size());           
            
            _log.info("Total Detenido Decimal: " + total_tiempo);
            
            //Para la variable total_tiempo, se convierte en milisegundos.
            long milisegundos = 0L;
            milisegundos += (int) total_tiempo * 3600000;
            milisegundos += (long) (((total_tiempo - (int)total_tiempo) * 60) * 60000);
                
            detenidoEnViaje.setTotal_tiempo(convertidorDeTiempo(milisegundos));
            
            float promedioTiempo = total_tiempo / listaDetenidoEnViaje.size();
            _log.info("Promedio detenido decimal: " + promedioTiempo);
            milisegundos = 0L;
            milisegundos += (int) promedioTiempo * 3600000;
            milisegundos += (long) (((promedioTiempo - (int) promedioTiempo) * 60) * 60000);
            
            detenidoEnViaje.setPromedio_detenido(this.convertidorDeTiempo(milisegundos));
            
            detenidoEnViaje.setListaDetenidoEnViaje(listaDetenidoEnViaje);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        /**
         * SELECT DEH.fechaentrada, EA.tipo, TE.nombre, EA.valor, (cerca_de_ciudad_orientacion_punto_interes_tipo_2(DEH.lng,DEH.lat,1, VEH.subflota, VEH.flota))
            FROM  entradaalarma as EA 
            INNER JOIN tipoevento as TE on EA.tipo = TE.tipo
            INNER JOIN datosentrada_his AS DEH on DEH.importacionid = EA.importacionid 
            INNER JOIN vehiculos as VEH on DEH.unitno = VEH.unitno
            WHERE DEH.flota='hesa' 
            AND DEH.viajeactivo = '820934' 
            AND DEH.unitno ='T904' 
            AND (EA.tipo=6 OR EA.tipo=7 OR EA.tipo=24 OR EA.tipo=25) -- 6=detenido y apagado, 7=detenido y encendido, 24=detendo, apagado en punto negro, 25=detenido, prendidoen punto negro 
            ORDER BY DEH.fechaentrada LIMIT 30
         *
         */
        return detenidoEnViaje;
    }

    /**
     * Obtiene una lista de las fallas que se reportaron para la unidad durante
     * un viaje especificado.
     *
     * @param unitno Identificador del viaje realizado por la unidad.
     * @param flota Nombre de la flota a la cual pertenece la unidad
     * directamente.
     * @param idViaje Identificador del viaje realizado por la unidad.
     * @return Lista de codigos de fallas.
     */
    public List<CodigoDeFalla> obtenerCodigoDeFalla(final String unitno, final String flota, final Long idViaje)
    {
        CodigoDeFalla codigoDeFalla;
        List<CodigoDeFalla> listaCodigoDeFalla = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");
                
        try 
        {
            Session s = null;
            SessionFactory sf = HibernateUtil2.getSessionFactory();
            s = sf.getCurrentSession();

            HibernateSentenceTransaction transaction = new HibernateSentenceTransaction(sf) 
            {
                @Override
                public Object transaction(BasicHibernateSupport bhs, Object... os) throws HibernateException, Exception 
                {
                    //Consulta que devuelve: viajeactivo, flota, unitno, fechaentrada, importacionid, fault_codes, descripcion, severidad.
                    String consultaSql = 
                    "SELECT \n"
                    + "DEH.viajeactivo, DEH.flota, DEH.unitno, DEH.fechaentrada,\n"
                    + "EF.importacionid, EF.fault_codes, EF.descripcion, EF.clasificacion_falla as severidad\n"
                    + "FROM entradafallas  as EF \n"
                    + "INNER JOIN datosentrada_his AS DEH on DEH.importacionid = EF.importacionid\n"
                    + "WHERE DEH.viajeactivo = '"+ idViaje +"'\n"
                    + "AND DEH.flota = '"+ flota +"'\n"
                    + "AND DEH.unitno = '"+ unitno +"'";
                    
                    List lista = bhs.getByNativeSQLQuery(consultaSql);
                    return lista;
                }
            };
            
            List<Object[]> list = (List<Object[]>) transaction.execute();           
           
            //Recorre la lista de la consulta obtenida
            for (Object[] o : list) 
            {
                codigoDeFalla = new CodigoDeFalla();
                
                /*
                    Asigna la severidad de acuerdo al valor entero recibido en la consulta:
                    0=Stop now, 1=Service now, 2=Service Soon y 3=Information only
                */
                switch((int)o[7])
                {
                    case 0: codigoDeFalla.setSeveridad("Stop now"); break;
                    case 1: codigoDeFalla.setSeveridad("Service now"); break;
                    case 2: codigoDeFalla.setSeveridad("Service soon"); break;
                    case 3: codigoDeFalla.setSeveridad("Information only"); break;
                }
                
                //Guarda la fecha como string debido al cambio de formato a: dd-MM-yyyy HH:mm
                codigoDeFalla.setFecha(dateFormat.format((Date)o[3]));
                
                //Guarda la descripcion de la falla
                codigoDeFalla.setDescripcion(o[6].toString());
            
                //Guarda el codigo de falla 
                codigoDeFalla.setCodigo((int)o[5]);
                
                //Agrega el objeto a la lista
                listaCodigoDeFalla.add(codigoDeFalla);
            }
            
        }
        
        catch (Exception e) { e.printStackTrace(); }

        //Severidad
        //0=Stop now, 1=Service now, 2=Service Soon y 3=Information only'
        /**
         * SELECT DEH.viajeactivo, DEH.flota, DEH.unitno, DEH.fechaentrada,
         * EF.importacionid, EF.fault_codes, EF.descripcion,
         * EF.clasificacion_falla as severidad FROM entradafallas as EF INNER
         * JOIN datosentrada_his AS DEH on DEH.importacionid = EF.importacionid
         * WHERE DEH.viajeactivo = '820934' AND DEH.flota = 'hesa' AND
         * DEH.unitno = 'T904'
         */
        return listaCodigoDeFalla;
    }
}

    
}

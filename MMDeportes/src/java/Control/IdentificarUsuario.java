/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Control;

import ConexionADatos.AccesoDatos;
import Modelo.Empleado;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
/**
 *
 * @author Mauricio
 */
@ManagedBean(name="IdentificarUsuario")
@SessionScoped
public class IdentificarUsuario implements Serializable {
    
    private String usuario;
    private String password;
    private Empleado empleado;
    
    public IdentificarUsuario() {
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    public void setEmpleado(Empleado empleado) {
        this.empleado = empleado;
    }

    public String login(){
        FacesContext facesContext = FacesContext.getCurrentInstance();
        String pagina = "";
        try {
            AccesoDatos conexion = new AccesoDatos();
            conexion.conectar();
            
            ArrayList resultado = conexion.ejecutarConsulta("Select * From Empleado as emp Where emp.usuario  = '"+ this.usuario+"' AND emp.contrasenia = '"+this.password+"'");            
            
            if(!resultado.isEmpty()){           
                HttpSession session =(HttpSession)facesContext.getExternalContext().getSession(false);
                                
                pagina = "index.xhtml";
                
            }else{
                
                pagina = "error.xhtml";
            }
            
        } catch (Exception ex) {
            Logger.getLogger(IdentificarUsuario.class.getName()).log(Level.SEVERE, null, ex);
        }

        return pagina;
    }
    
    public void logout() throws IOException{
        
        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpSession session;
        ServletContext oServletContext=(ServletContext)facesContext.getExternalContext().getContext();
        String nombreProyecto=oServletContext.getContextPath()+"/";
        session = (HttpSession)facesContext.getExternalContext().getSession(false);
        
        if (session != null){
            session.invalidate();
        }
        
        facesContext.getExternalContext().redirect(nombreProyecto);
    }
}

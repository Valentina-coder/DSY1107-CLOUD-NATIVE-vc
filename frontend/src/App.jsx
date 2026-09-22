import React, { useState, useEffect } from "react";
import { useIsAuthenticated, useMsal } from "@azure/msal-react";
import api, { setAuthToken } from "./services/api";
import { BannerSeguridad } from "./components/BannerSeguridad";
import { loginRequest } from "./auth/authConfig";

const CATEGORIAS_DEFAULT = [
  { id: 1, nombre: "Iluminación" },
  { id: 2, nombre: "Hogar y Muebles" },
  { id: 3, nombre: "Electrónica" }
];

export function App() {
  const { instance, accounts } = useMsal();
  const isAuthenticated = useIsAuthenticated();
  const user = accounts[0];
  
  const [categorias, setCategorias] = useState(CATEGORIAS_DEFAULT);
  const [productos, setProductos] = useState([]);
  const [lastStatus, setLastStatus] = useState(null);
  
  const [nuevaCategoria, setNuevaCategoria] = useState("");
  const [nuevoProducto, setNuevoProducto] = useState({ nombre: "", precio: "", categoriaId: "1" });

  useEffect(() => {
    const obtenerToken = async () => {
      if (isAuthenticated) {
        try {
          const response = await instance.acquireTokenSilent({
            ...loginRequest,
            account: accounts[0],
          });
          const token = response.accessToken;
          setAuthToken(token);
        } catch (error) {
          console.error("Error obteniendo el token:", error);
          setAuthToken(null);
        }
      } else {
        setAuthToken(null);
      }
    };
    obtenerToken();
  }, [accounts, instance, isAuthenticated]);

  // 1. CONSULTAR CATEGORÍAS
  const cargarCategorias = async () => {
    try {
      const res = await api.get("/api/public/categorias");
      if (Array.isArray(res.data) && res.data.length > 0) {
        setCategorias(res.data);
      } else {
        setCategorias(CATEGORIAS_DEFAULT);
      }
      setLastStatus(res.status);
    } catch (err) {
      setLastStatus(err.response?.status || 500);
      setCategorias(CATEGORIAS_DEFAULT);
    }
  };

  // 2. CREAR CATEGORÍA
  const crearCategoria = async (e) => {
    e.preventDefault();
    if (!nuevaCategoria.trim()) return;

    try {
      const payload = { nombre: nuevaCategoria };
      const res = await api.post("/api/admin/categorias", payload);
      setLastStatus(res.status);
      setNuevaCategoria("");
      cargarCategorias();
    } catch (err) {
      setLastStatus(err.response?.status || 500);
      setNuevaCategoria("");
    }
  };

  // 3. CONSULTAR PRODUCTOS
  const cargarProductos = async () => {
    try {
      const res = await api.get("/api/productos");
      if (Array.isArray(res.data) && res.data.length > 0) {
        setProductos(res.data);
      }
      setLastStatus(res.status);
    } catch (err) {
      setLastStatus(err.response?.status || 500);
    }
  };

  // 4. CREAR PRODUCTO
  const crearProducto = async (e) => {
    e.preventDefault();
    if (!nuevoProducto.nombre || !nuevoProducto.precio || !nuevoProducto.categoriaId) return;

    try {
      const payload = {
        nombre: nuevoProducto.nombre,
        precio: parseFloat(nuevoProducto.precio),
        categoria: { id: parseInt(nuevoProducto.categoriaId) }
      };
      const res = await api.post("/api/admin/productos", payload);
      setLastStatus(res.status);
      cargarProductos();
    } catch (err) {
      setLastStatus(err.response?.status || 500);
    }

    setNuevoProducto({ nombre: "", precio: "", categoriaId: categorias[0]?.id || "1" });
  };

  useEffect(() => {
    cargarCategorias();
  }, []);

  return (
    <div style={{ maxWidth: "800px", margin: "0 auto", padding: "20px", fontFamily: "Segoe UI, sans-serif" }}>
      <header style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "20px" }}>
        <h1>Stock360</h1>
        {isAuthenticated ? (
          <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
            <span>{user?.name || user?.email}</span>
            <button onClick={() => logout({ logoutParams: { returnTo: window.location.origin } })} style={{ padding: "8px 16px", cursor: "pointer" }}>
              Cerrar Sesión
            </button>
          </div>
        ) : (
          <button onClick={() => loginWithRedirect()} style={{ padding: "8px 16px", cursor: "pointer" }}>
            Iniciar Sesión
          </button>
        )}
      </header>

      <BannerSeguridad lastStatus={lastStatus} />

      {/* 1. SECCIÓN CATEGORÍAS */}
      <section style={{ marginBottom: "24px", background: "#f8fafc", padding: "16px", borderRadius: "8px" }}>
        <h2>1. Categorías Disponibles (Público)</h2>
        <button onClick={cargarCategorias} style={{ padding: "8px 16px", marginBottom: "12px", cursor: "pointer" }}>
          Consultar Categorías
        </button>
        
        <ul>
          {categorias.map((c) => (
            <li key={c.id}><strong>ID {c.id}:</strong> {c.nombre}</li>
          ))}
        </ul>

        {isAuthenticated && (
          <form onSubmit={crearCategoria} style={{ display: "flex", gap: "10px", marginTop: "12px" }}>
            <input 
              placeholder="Nueva Categoría (ej: Herramientas)" 
              value={nuevaCategoria}
              onChange={(e) => setNuevaCategoria(e.target.value)} 
              required 
              style={{ padding: "8px", flex: 1 }}
            />
            <button type="submit" style={{ padding: "8px 16px", background: "#10b981", color: "white", border: "none", borderRadius: "4px", cursor: "pointer" }}>
              + Crear Categoría
            </button>
          </form>
        )}
      </section>

      {/* 2. SECCIÓN PRODUCTOS */}
      <section style={{ marginBottom: "24px", background: "#f8fafc", padding: "16px", borderRadius: "8px" }}>
        <h2>2. Productos (Lectura Protegida)</h2>
        <button onClick={cargarProductos} style={{ padding: "8px 16px", marginBottom: "12px", cursor: "pointer" }}>
          Consultar Productos
        </button>
        
        {productos.length === 0 ? (
          <p style={{ color: "#64748b" }}>Presiona "Consultar Productos" para listar el inventario.</p>
        ) : (
          <ul style={{ marginTop: "12px" }}>
            {productos.map((p) => (
              <li key={p.id} style={{ marginBottom: "6px" }}>
                <strong>{p.nombre}</strong> — ${p.precio} | <em>Categoría: {p.categoria?.nombre || "N/A"}</em>
              </li>
            ))}
          </ul>
        )}
      </section>

      {/* 3. CREAR PRODUCTO */}
      {isAuthenticated && (
        <section style={{ background: "#f8fafc", padding: "16px", borderRadius: "8px" }}>
          <h2>3. Crear Producto (Escritura Admin)</h2>
          <form onSubmit={crearProducto} style={{ display: "flex", flexDirection: "column", gap: "10px", maxWidth: "400px" }}>
            <input 
              placeholder="Nombre del Producto" 
              value={nuevoProducto.nombre}
              onChange={(e) => setNuevoProducto({ ...nuevoProducto, nombre: e.target.value })} 
              required 
              style={{ padding: "8px" }}
            />
            <input 
              placeholder="Precio" 
              type="number" 
              value={nuevoProducto.precio}
              onChange={(e) => setNuevoProducto({ ...nuevoProducto, precio: e.target.value })} 
              required 
              style={{ padding: "8px" }}
            />
            
            <select 
              value={nuevoProducto.categoriaId}
              onChange={(e) => setNuevoProducto({ ...nuevoProducto, categoriaId: e.target.value })} 
              required
              style={{ padding: "8px" }}
            >
              <option value="">Seleccione Categoría</option>
              {categorias.map((c) => (
                <option key={c.id} value={c.id}>{c.nombre}</option>
              ))}
            </select>

            <button type="submit" style={{ padding: "10px", background: "#2563eb", color: "white", border: "none", borderRadius: "4px", cursor: "pointer" }}>
              Guardar en Inventario
            </button>
          </form>
        </section>
      )}
    </div>
  );
}

export default App;
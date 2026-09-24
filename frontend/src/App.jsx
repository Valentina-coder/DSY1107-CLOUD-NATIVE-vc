import React, { useState, useEffect } from "react";
import { useAuth0 } from "@auth0/auth0-react";
import api, { setAuthToken } from "./services/api";
import { BannerSeguridad } from "./components/BannerSeguridad";

const CATEGORIAS_DEFAULT = [
  { id: 1, nombre: "Iluminación" },
  { id: 2, nombre: "Hogar y Muebles" },
  { id: 3, nombre: "Electrónica" }
];

export function App() {
  const { isAuthenticated, user, loginWithPopup, logout, getAccessTokenSilently, isLoading } = useAuth0();

  const [categorias, setCategorias] = useState(CATEGORIAS_DEFAULT);
  const [productos, setProductos] = useState([]);
  const [lastStatus, setLastStatus] = useState(null);

  // Estados de formularios
  const [nuevaCategoria, setNuevaCategoria] = useState("");
  const [nuevoProducto, setNuevoProducto] = useState({ nombre: "", precio: "", categoriaId: "1" });
  
  // Estado para la edición de producto
  const [productoEditando, setProductoEditando] = useState(null);

  // Helper para asegurar token en llamadas protegidas
  const getTokenHeaders = async () => {
    try {
      const token = await getAccessTokenSilently();
      setAuthToken(token);
      return { headers: { Authorization: `Bearer ${token}` } };
    } catch (e) {
      console.error("Error al obtener token silencioso:", e);
      return {};
    }
  };

  // Sincronizar Token al iniciar sesión
  useEffect(() => {
    const sincronizarToken = async () => {
      if (isAuthenticated) {
        await getTokenHeaders();
      } else {
        setAuthToken(null);
      }
    };
    sincronizarToken();
  }, [isAuthenticated]);

  const handleLogin = async () => {
    try {
      await loginWithPopup();
    } catch (error) {
      console.error("Error al iniciar sesión:", error);
    }
  };

  const handleLogout = () => {
    setAuthToken(null);
    setProductos([]);
    logout({ logoutParams: { returnTo: window.location.origin } });
  };

  // -------------------------------------------------------------
  // 1. OPERACIONES DE CATEGORÍAS
  // -------------------------------------------------------------
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

  const crearCategoria = async (e) => {
    e.preventDefault();
    if (!nuevaCategoria.trim()) return;

    try {
      const authConfig = await getTokenHeaders();
      const payload = { nombre: nuevaCategoria };
      const res = await api.post("/api/admin/categorias", payload, authConfig);
      setLastStatus(res.status);
      setNuevaCategoria("");
      cargarCategorias();
    } catch (err) {
      setLastStatus(err.response?.status || 500);
    }
  };

  // -------------------------------------------------------------
  // 2. OPERACIONES DE PRODUCTOS (CRUD COMPLETO)
  // -------------------------------------------------------------
  
  // GET: Consultar Productos
  const cargarProductos = async () => {
    try {
      const authConfig = await getTokenHeaders();
      const res = await api.get("/api/productos", authConfig);
      if (Array.isArray(res.data)) {
        setProductos(res.data);
      }
      setLastStatus(res.status);
    } catch (err) {
      setLastStatus(err.response?.status || 500);
    }
  };

  // POST: Crear Producto
  const crearProducto = async (e) => {
    e.preventDefault();
    if (!nuevoProducto.nombre || !nuevoProducto.precio || !nuevoProducto.categoriaId) return;

    try {
      const authConfig = await getTokenHeaders();
      const payload = {
        nombre: nuevoProducto.nombre,
        precio: parseFloat(nuevoProducto.precio),
        categoria: { id: parseInt(nuevoProducto.categoriaId) }
      };
      const res = await api.post("/api/admin/productos", payload, authConfig);
      setLastStatus(res.status);
      setNuevoProducto({ nombre: "", precio: "", categoriaId: categorias[0]?.id ? String(categorias[0].id) : "1" });
      cargarProductos();
    } catch (err) {
      setLastStatus(err.response?.status || 500);
    }
  };

  // PUT: Actualizar Producto
  const actualizarProducto = async (e) => {
    e.preventDefault();
    if (!productoEditando) return;

    try {
      const authConfig = await getTokenHeaders();
      const payload = {
        nombre: productoEditando.nombre,
        precio: parseFloat(productoEditando.precio),
        categoria: { id: parseInt(productoEditando.categoriaId) }
      };
      const res = await api.put(`/api/admin/productos/${productoEditando.id}`, payload, authConfig);
      setLastStatus(res.status);
      setProductoEditando(null);
      cargarProductos();
    } catch (err) {
      setLastStatus(err.response?.status || 500);
    }
  };

  // DELETE: Eliminar Producto
  const eliminarProducto = async (id) => {
    if (!window.confirm("¿Estás seguro de que deseas eliminar este producto?")) return;

    try {
      const authConfig = await getTokenHeaders();
      const res = await api.delete(`/api/admin/productos/${id}`, authConfig);
      setLastStatus(res.status);
      cargarProductos();
    } catch (err) {
      setLastStatus(err.response?.status || 500);
    }
  };

  // Iniciar la edición de un producto existente
  const prepararEdicion = (prod) => {
    setProductoEditando({
      id: prod.id,
      nombre: prod.nombre,
      precio: prod.precio,
      categoriaId: prod.categoria?.id ? String(prod.categoria.id) : "1"
    });
  };

  useEffect(() => {
    cargarCategorias();
  }, []);

  if (isLoading) {
    return <div style={{ padding: "40px", textAlign: "center" }}>Cargando estado de autenticación...</div>;
  }

  return (
    <div style={{ maxWidth: "800px", margin: "0 auto", padding: "20px", fontFamily: "Segoe UI, sans-serif" }}>
      <header style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "20px" }}>
        <h1>Stock360</h1>
        {isAuthenticated ? (
          <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
            <span>{user?.name || user?.email}</span>
            <button onClick={handleLogout} style={{ padding: "8px 16px", cursor: "pointer" }}>
              Cerrar Sesión
            </button>
          </div>
        ) : (
          <button onClick={handleLogin} style={{ padding: "8px 16px", cursor: "pointer" }}>
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

      {/* 2. SECCIÓN PRODUCTOS & EDICIÓN */}
      <section style={{ marginBottom: "24px", background: "#f8fafc", padding: "16px", borderRadius: "8px" }}>
        <h2>2. Productos (Lectura y Gestión)</h2>
        <button onClick={cargarProductos} style={{ padding: "8px 16px", marginBottom: "12px", cursor: "pointer" }}>
          Consultar Productos
        </button>
        
        {/* Formulario de Edición de Producto Modal / Inline */}
        {productoEditando && (
          <div style={{ background: "#e2e8f0", padding: "12px", borderRadius: "6px", marginBottom: "16px" }}>
            <h3 style={{ marginTop: 0 }}>Editar Producto (ID: {productoEditando.id})</h3>
            <form onSubmit={actualizarProducto} style={{ display: "flex", gap: "10px", flexWrap: "wrap" }}>
              <input 
                placeholder="Nombre" 
                value={productoEditando.nombre}
                onChange={(e) => setProductoEditando({ ...productoEditando, nombre: e.target.value })} 
                required 
                style={{ padding: "6px", flex: "1 1 150px" }}
              />
              <input 
                placeholder="Precio" 
                type="number"
                value={productoEditando.precio}
                onChange={(e) => setProductoEditando({ ...productoEditando, precio: e.target.value })} 
                required 
                style={{ padding: "6px", width: "100px" }}
              />
              <select 
                value={productoEditando.categoriaId}
                onChange={(e) => setProductoEditando({ ...productoEditando, categoriaId: e.target.value })} 
                style={{ padding: "6px" }}
              >
                {categorias.map((c) => (
                  <option key={c.id} value={c.id}>{c.nombre}</option>
                ))}
              </select>
              <button type="submit" style={{ padding: "6px 12px", background: "#d97706", color: "white", border: "none", borderRadius: "4px", cursor: "pointer" }}>
                Guardar Cambios
              </button>
              <button type="button" onClick={() => setProductoEditando(null)} style={{ padding: "6px 12px", background: "#64748b", color: "white", border: "none", borderRadius: "4px", cursor: "pointer" }}>
                Cancelar
              </button>
            </form>
          </div>
        )}

        {productos.length === 0 ? (
          <p style={{ color: "#64748b" }}>Presiona "Consultar Productos" para listar el inventario.</p>
        ) : (
          <ul style={{ marginTop: "12px", paddingLeft: 0, listStyle: "none" }}>
            {productos.map((p) => (
              <li key={p.id} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "8px 0", borderBottom: "1px solid #e2e8f0" }}>
                <div>
                  <strong>{p.nombre}</strong> — ${p.precio} | <em>Categoría: {p.categoria?.nombre || "N/A"}</em>
                </div>
                {isAuthenticated && (
                  <div style={{ display: "flex", gap: "6px" }}>
                    <button 
                      onClick={() => prepararEdicion(p)} 
                      style={{ padding: "4px 8px", background: "#f59e0b", color: "white", border: "none", borderRadius: "4px", cursor: "pointer", fontSize: "12px" }}
                    >
                      Editar
                    </button>
                    <button 
                      onClick={() => eliminarProducto(p.id)} 
                      style={{ padding: "4px 8px", background: "#ef4444", color: "white", border: "none", borderRadius: "4px", cursor: "pointer", fontSize: "12px" }}
                    >
                      Eliminar
                    </button>
                  </div>
                )}
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
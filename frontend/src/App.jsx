import React, { useState, useEffect, useRef } from "react";
import api, { setAuthToken, setAuthTokenProvider } from "./services/api";
import { BannerSeguridad } from "./components/BannerSeguridad";
import {
  firebaseAuth,
  googleProvider,
  onAuthStateChanged,
  signInWithPopup,
  signOut,
} from "./auth/firebase";

export function App() {
  const [user, setUser] = useState(null);
  const [authError, setAuthError] = useState(null);
  const isAuthenticated = Boolean(user);
  
  const [categorias, setCategorias] = useState([]);
  const [productos, setProductos] = useState([]);
  const [lastStatus, setLastStatus] = useState(null);
  const [lastError, setLastError] = useState(null);
  
  const [nuevaCategoria, setNuevaCategoria] = useState("");
  const [categoriaEnEdicion, setCategoriaEnEdicion] = useState(null);
  const [nuevoProducto, setNuevoProducto] = useState({ nombre: "", precio: "", categoriaId: "1" });
  const [productoEnEdicion, setProductoEnEdicion] = useState(null);
  const [productoDetalle, setProductoDetalle] = useState(null);
  const formularioProductoRef = useRef(null);

  useEffect(() => {
    return onAuthStateChanged(firebaseAuth, (currentUser) => {
      setUser(currentUser);
      if (currentUser) {
        const getApiToken = () => currentUser.getIdToken();
        setAuthTokenProvider(getApiToken);
        getApiToken()
          .then((token) => setAuthToken(token))
          .catch((tokenError) => setAuthError(tokenError));
      } else {
        setAuthTokenProvider(null);
        setAuthToken(null);
      }
    });
  }, []);

  const iniciarSesion = async () => {
    try {
      setAuthError(null);
      await signInWithPopup(firebaseAuth, googleProvider);
    } catch (loginError) {
      setAuthError(loginError);
    }
  };

  const cerrarSesion = async () => {
    try {
      await signOut(firebaseAuth);
    } catch (logoutError) {
      setAuthError(logoutError);
    }
  };

  // 1. CONSULTAR CATEGORÍAS
  const cargarCategorias = async (actualizarEstado = true) => {
    try {
      const res = await api.get("/api/public/categorias");
      if (Array.isArray(res.data) && res.data.length > 0) {
        setCategorias(res.data);
      }
      if (actualizarEstado) setLastStatus(res.status);
    } catch (err) {
      setLastStatus(err.response?.status || null);
      setLastError(err.response?.data?.message || `No se pudo consultar las categorías (${err.message})`);
      setCategorias([]);
    }
  };

  // 2. CREAR CATEGORÍA
  const crearCategoria = async (e) => {
    e.preventDefault();
    if (!nuevaCategoria.trim()) return;

    try {
      const payload = { nombre: nuevaCategoria };
      const res = await api.post("/api/categorias", payload);
      setLastStatus(res.status);
      setLastError(null);
      setNuevaCategoria("");
      await cargarCategorias(false);
      setLastStatus(res.status);
    } catch (err) {
      setLastStatus(err.response?.status || null);
      setLastError(err.response?.data?.message || `No se pudo crear la categoría (${err.message})`);
    }
  };

  const guardarCategoria = async (e) => {
    e.preventDefault();
    if (!nuevaCategoria.trim()) return;

    try {
      const res = await api.put(`/api/categorias/${categoriaEnEdicion.id}`, { nombre: nuevaCategoria });
      setLastStatus(res.status);
      setLastError(null);
      setCategoriaEnEdicion(null);
      setNuevaCategoria("");
      cargarCategorias();
    } catch (err) {
      setLastStatus(err.response?.status || null);
      setLastError(err.response?.data?.message || `No se pudo actualizar la categoría (${err.message})`);
    }
  };

  const eliminarCategoria = async (id) => {
    if (!window.confirm("¿Eliminar esta categoría?")) return;

    try {
      const res = await api.delete(`/api/categorias/${id}`);
      setLastStatus(res.status);
      setLastError(null);
      setCategorias((categoriasActuales) => categoriasActuales.filter((categoria) => categoria.id !== id));
    } catch (err) {
      setLastStatus(err.response?.status || null);
      setLastError(err.response?.data?.message || `No se pudo eliminar la categoría (${err.message})`);
    }
  };

  // 3. CONSULTAR PRODUCTOS
  const cargarProductos = async (actualizarEstado = true) => {
    try {
      const res = await api.get("/api/productos");
      if (Array.isArray(res.data) && res.data.length > 0) {
        setProductos(res.data);
      }
      if (actualizarEstado) setLastStatus(res.status);
    } catch (err) {
      setLastStatus(err.response?.status || null);
      setLastError(err.response?.data?.message || `No se pudo consultar los productos (${err.message})`);
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
      const res = await api.post("/api/productos", payload);
      setLastStatus(res.status);
      await cargarProductos(false);
      setLastStatus(res.status);
    } catch (err) {
      setLastStatus(err.response?.status || 500);
    }

    setNuevoProducto({ nombre: "", precio: "", categoriaId: categorias[0]?.id || "1" });
  };

  const verDetalleProducto = async (id) => {
    try {
      const res = await api.get(`/api/productos/${id}`);
      setProductoDetalle(res.data);
      setLastStatus(res.status);
      setLastError(null);
    } catch (err) {
      setLastStatus(err.response?.status || null);
      setLastError(err.response?.data?.message || `No se pudo consultar el detalle (${err.message})`);
    }
  };

  const iniciarEdicionProducto = (producto) => {
    setProductoEnEdicion(producto);
    setNuevoProducto({
      nombre: producto.nombre,
      precio: String(producto.precio),
      categoriaId: String(producto.categoria?.id || categorias[0]?.id || "1")
    });
    requestAnimationFrame(() => formularioProductoRef.current?.scrollIntoView({ behavior: "smooth", block: "center" }));
  };

  const cancelarEdicionProducto = () => {
    setProductoEnEdicion(null);
    setNuevoProducto({ nombre: "", precio: "", categoriaId: categorias[0]?.id || "1" });
  };

  const actualizarProducto = async (e) => {
    e.preventDefault();
    if (!productoEnEdicion || !nuevoProducto.nombre || !nuevoProducto.precio || !nuevoProducto.categoriaId) return;

    try {
      const payload = {
        nombre: nuevoProducto.nombre,
        precio: parseFloat(nuevoProducto.precio),
        categoria: { id: parseInt(nuevoProducto.categoriaId, 10) }
      };
      const res = await api.put(`/api/productos/${productoEnEdicion.id}`, payload);
      setLastStatus(res.status);
      setLastError(null);
      setProductoEnEdicion(null);
      setNuevoProducto({ nombre: "", precio: "", categoriaId: categorias[0]?.id || "1" });
      cargarProductos();
    } catch (err) {
      setLastStatus(err.response?.status || null);
      setLastError(err.response?.data?.message || `No se pudo actualizar el producto (${err.message})`);
    }
  };

  const eliminarProducto = async (id) => {
    if (!window.confirm("¿Eliminar este producto?")) return;

    try {
      const res = await api.delete(`/api/productos/${id}`);
      setLastStatus(res.status);
      setProductos((productosActuales) => productosActuales.filter((producto) => producto.id !== id));
    } catch (err) {
      setLastStatus(err.response?.status || 500);
    }
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
            <span>{user?.displayName || user?.email}</span>
            <button onClick={cerrarSesion} style={{ padding: "8px 16px", cursor: "pointer" }}>
              Cerrar Sesión
            </button>
          </div>
        ) : (
          <button onClick={iniciarSesion} style={{ padding: "8px 16px", cursor: "pointer" }}>
            Iniciar Sesión
          </button>
        )}
      </header>

      <BannerSeguridad lastStatus={lastStatus} lastError={lastError} isAuthenticated={isAuthenticated} user={user} />

      {authError && (
        <p role="alert" style={{ color: "#b91c1c", background: "#fee2e2", padding: "12px", borderRadius: "6px" }}>
          Error de autenticación: {authError.message}
        </p>
      )}

      {/* 1. SECCIÓN CATEGORÍAS */}
      <section style={{ marginBottom: "24px", background: "#f8fafc", padding: "16px", borderRadius: "8px" }}>
        <h2>1. Categorías Disponibles (Público)</h2>
        <button onClick={cargarCategorias} style={{ padding: "8px 16px", marginBottom: "12px", cursor: "pointer" }}>
          Consultar Categorías
        </button>
        
        <ul>
          {categorias.map((c) => (
            <li key={c.id}>
              <strong>ID {c.id}:</strong> {c.nombre}
              {isAuthenticated && (
                <span style={{ marginLeft: "12px" }}>
                  <button type="button" onClick={() => { setCategoriaEnEdicion(c); setNuevaCategoria(c.nombre); }}>Editar</button>{" "}
                  <button type="button" onClick={() => eliminarCategoria(c.id)}>Eliminar</button>
                </span>
              )}
            </li>
          ))}
        </ul>

        {isAuthenticated && (
          <form onSubmit={categoriaEnEdicion ? guardarCategoria : crearCategoria} style={{ display: "flex", gap: "10px", marginTop: "12px" }}>
            <input 
              placeholder={categoriaEnEdicion ? "Nombre de la categoría" : "Nueva Categoría (ej: Herramientas)"}
              value={nuevaCategoria}
              onChange={(e) => setNuevaCategoria(e.target.value)} 
              required 
              style={{ padding: "8px", flex: 1 }}
            />
            <button type="submit" style={{ padding: "8px 16px", background: "#10b981", color: "white", border: "none", borderRadius: "4px", cursor: "pointer" }}>
              {categoriaEnEdicion ? "Guardar cambios" : "+ Crear Categoría"}
            </button>
            {categoriaEnEdicion && (
              <button type="button" onClick={() => { setCategoriaEnEdicion(null); setNuevaCategoria(""); }}>Cancelar</button>
            )}
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
                {isAuthenticated && (
                  <span style={{ marginLeft: "12px" }}>
                    <button type="button" onClick={() => verDetalleProducto(p.id)}>Ver detalle</button>{" "}
                    <button type="button" onClick={() => iniciarEdicionProducto(p)}>Editar</button>{" "}
                    <button type="button" onClick={() => eliminarProducto(p.id)}>Eliminar</button>
                  </span>
                )}
              </li>
            ))}
          </ul>
        )}

        {productoDetalle && (
          <div style={{ marginTop: "16px", padding: "12px", background: "#e2e8f0", borderRadius: "6px" }}>
            <strong>Detalle del producto #{productoDetalle.id}</strong>
            <p>Nombre: {productoDetalle.nombre}</p>
            <p>Precio: ${productoDetalle.precio}</p>
            <p>Categoría: {productoDetalle.categoria?.nombre || "N/A"}</p>
            <button type="button" onClick={() => setProductoDetalle(null)}>Cerrar detalle</button>
          </div>
        )}
      </section>

      {/* 3. CREAR PRODUCTO */}
      {isAuthenticated && (
        <section ref={formularioProductoRef} style={{ background: "#f8fafc", padding: "16px", borderRadius: "8px" }}>
          <h2>{productoEnEdicion ? "3. Editar Producto" : "3. Crear Producto"} (Escritura protegida)</h2>
          <form onSubmit={productoEnEdicion ? actualizarProducto : crearProducto} style={{ display: "flex", flexDirection: "column", gap: "10px", maxWidth: "400px" }}>
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
              {productoEnEdicion ? "Actualizar producto" : "Guardar en Inventario"}
            </button>
            {productoEnEdicion && (
              <button type="button" onClick={cancelarEdicionProducto}>Cancelar edición</button>
            )}
          </form>
        </section>
      )}
    </div>
  );
}

export default App;
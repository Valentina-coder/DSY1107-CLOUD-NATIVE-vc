import React, { useState, useEffect, useCallback, useMemo } from "react";
import { useAuth0 } from "@auth0/auth0-react";
import api, { setAuthToken } from "./services/api";
import { BannerSeguridad } from "./components/BannerSeguridad";
import { auth0Config } from "./auth/auth0Config";
import "./App.css";

const CATEGORIAS_DEFAULT = [
  { id: 1, nombre: "Iluminación" },
  { id: 2, nombre: "Hogar y Muebles" },
  { id: 3, nombre: "Electrónica" },
];

const ICON_PATHS = {
  box: <><path d="m12 3 9 5-9 5-9-5 9-5Z" /><path d="m3 8 9 5 9-5" /><path d="M3 8v8l9 5 9-5V8" /><path d="M12 13v8" /></>,
  grid: <><rect x="3" y="3" width="7" height="7" rx="1.5" /><rect x="14" y="3" width="7" height="7" rx="1.5" /><rect x="14" y="14" width="7" height="7" rx="1.5" /><rect x="3" y="14" width="7" height="7" rx="1.5" /></>,
  tag: <><path d="M20.6 13.4 13.4 20.6a2 2 0 0 1-2.8 0L3 13V3h10l7.6 7.6a2 2 0 0 1 0 2.8Z" /><circle cx="7.5" cy="7.5" r="1.5" /></>,
  chart: <><path d="M3 3v18h18" /><path d="m19 9-5 5-4-4-5 5" /></>,
  plus: <><path d="M12 5v14" /><path d="M5 12h14" /></>,
  search: <><circle cx="11" cy="11" r="7" /><path d="m20 20-4-4" /></>,
  refresh: <><path d="M20 7v5h-5" /><path d="M4 17v-5h5" /><path d="M5.6 9A7 7 0 0 1 18 6l2 1" /><path d="M18.4 15A7 7 0 0 1 6 18l-2-1" /></>,
  logout: <><path d="M10 17l5-5-5-5" /><path d="M15 12H3" /><path d="M12 3h6a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-6" /></>,
  edit: <><path d="M12 20h9" /><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L8 18l-4 1 1-4Z" /></>,
  trash: <><path d="M3 6h18" /><path d="M8 6V4h8v2" /><path d="m19 6-1 14H6L5 6" /><path d="M10 11v5M14 11v5" /></>,
  arrow: <><path d="M5 12h14" /><path d="m12 5 7 7-7 7" /></>,
  lock: <><rect x="4" y="10" width="16" height="11" rx="2" /><path d="M8 10V7a4 4 0 0 1 8 0v3" /></>,
  close: <><path d="m18 6-12 12M6 6l12 12" /></>,
};

function Icon({ name, size = 18 }) {
  return (
    <svg
      aria-hidden="true"
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      {ICON_PATHS[name]}
    </svg>
  );
}

export function App() {
  const {
    isAuthenticated,
    user,
    loginWithRedirect,
    logout,
    getAccessTokenSilently,
    isLoading,
    error: authError,
  } = useAuth0();

  const [categorias, setCategorias] = useState(CATEGORIAS_DEFAULT);
  const [productos, setProductos] = useState([]);
  const [lastStatus, setLastStatus] = useState(null);
  const [feedback, setFeedback] = useState("");
  const [authActionPending, setAuthActionPending] = useState(false);

  // Estados de formularios
  const [nuevaCategoria, setNuevaCategoria] = useState("");
  const [nuevoProducto, setNuevoProducto] = useState({ nombre: "", precio: "", categoriaId: "1" });

  // Estado para la edición de producto
  const [productoEditando, setProductoEditando] = useState(null);
  const [busqueda, setBusqueda] = useState("");
  const [categoriaFiltro, setCategoriaFiltro] = useState("todas");

  // Helper para asegurar token JWT legítimo en llamadas protegidas
  const getTokenHeaders = useCallback(async () => {
    try {
      const token = await getAccessTokenSilently({
        authorizationParams: {
          audience: auth0Config.audience,
          scope: auth0Config.scope,
        }
      });
      setAuthToken(token);
      return { headers: { Authorization: `Bearer ${token}` } };
    } catch (e) {
      setAuthToken(null);
      console.error("Error al obtener JWT desde Auth0:", e);
      throw e;
    }
  }, [getAccessTokenSilently]);

  const registrarError = useCallback((accion, error) => {
    console.error(accion, error.response || error);
    const status = error.response?.status ?? null;
    const responseData = error.response?.data;
    const detail = typeof responseData === "string"
      ? responseData
      : responseData?.message || error.message || "Error desconocido";
    const guidance = status === 401
      ? "Vuelve a iniciar sesión para obtener un token válido."
      : status === 403
        ? "Tu usuario necesita el permiso recurso.write en Auth0."
        : error.isAxiosError
          ? "No se pudo conectar con el backend local."
          : detail;
    setLastStatus(status);
    setFeedback(`No se pudo ${accion}: ${guidance}`);
  }, []);

  // Sincronizar Token al iniciar sesión
  useEffect(() => {
    const sincronizarToken = async () => {
      if (isAuthenticated) {
        try {
          await getTokenHeaders();
        } catch (error) {
          registrarError("obtener un token de Auth0", error);
        }
      } else {
        setAuthToken(null);
      }
    };
    sincronizarToken();
  }, [isAuthenticated, getTokenHeaders, registrarError]);

  const handleLogin = async () => {
    setAuthActionPending(true);
    setFeedback("");
    try {
      await loginWithRedirect();
    } catch (error) {
      console.error("Error al iniciar sesión:", error);
      setAuthActionPending(false);
      setFeedback(`No se pudo iniciar sesión: ${error.message || "Error inesperado de Auth0."}`);
    }
  };

  const handleLogout = async () => {
    setAuthActionPending(true);
    setAuthToken(null);
    setProductos([]);
    try {
      await logout({ logoutParams: { returnTo: window.location.origin } });
    } catch (error) {
      console.error("Error al cerrar sesión:", error);
      setAuthActionPending(false);
      setFeedback(`No se pudo cerrar la sesión: ${error.message || "Error inesperado de Auth0."}`);
    }
  };

  // -------------------------------------------------------------
  // 1. OPERACIONES DE CATEGORÍAS
  // -------------------------------------------------------------
  const cargarCategorias = useCallback(async () => {
    try {
      const res = await api.get("/api/public/categorias");
      setCategorias(Array.isArray(res.data) ? res.data : []);
      setLastStatus(res.status);
      setFeedback("Categorías cargadas.");
    } catch (err) {
      registrarError("cargar las categorías", err);
      setCategorias(CATEGORIAS_DEFAULT);
    }
  }, [registrarError]);

  const crearCategoria = async (e) => {
    e.preventDefault();
    if (!nuevaCategoria.trim()) {
      setFeedback("Escribe un nombre para la categoría.");
      return;
    }

    try {
      const authConfig = await getTokenHeaders();
      const payload = { nombre: nuevaCategoria.trim() };
      const res = await api.post("/api/admin/categorias", payload, authConfig);
      setLastStatus(res.status);
      setFeedback("Categoría guardada correctamente.");
      setNuevaCategoria("");
      await cargarCategorias();
    } catch (err) {
      registrarError("guardar la categoría", err);
    }
  };

  // -------------------------------------------------------------
  // 2. OPERACIONES DE PRODUCTOS (CRUD COMPLETO)
  // -------------------------------------------------------------

  // GET: Consultar Productos
  const cargarProductos = useCallback(async () => {
    if (!isAuthenticated) {
      setFeedback("Inicia sesión con Auth0 para consultar el inventario de productos.");
      return;
    }

    try {
      const authConfig = await getTokenHeaders();
      const res = await api.get("/api/productos", authConfig);
      if (Array.isArray(res.data)) {
        setProductos(res.data);
      }
      setLastStatus(res.status);
      setFeedback("Productos cargados.");
    } catch (err) {
      registrarError("cargar los productos", err);
    }
  }, [getTokenHeaders, isAuthenticated, registrarError]);

  // POST: Crear Producto
  const crearProducto = async (e) => {
    e.preventDefault();
    const catId = parseInt(nuevoProducto.categoriaId, 10);
    const precio = Number(nuevoProducto.precio);
    if (!nuevoProducto.nombre.trim() || !Number.isFinite(precio) || precio < 0 || !Number.isInteger(catId)) {
      setFeedback("Completa el nombre, un precio válido y una categoría.");
      return;
    }

    try {
      const authConfig = await getTokenHeaders();
      const payload = {
        nombre: nuevoProducto.nombre,
        precio,
        categoria: { id: catId }
      };
      const res = await api.post("/api/admin/productos", payload, authConfig);
      setLastStatus(res.status);
      setFeedback("Producto guardado correctamente.");
      setNuevoProducto({
        nombre: "",
        precio: "",
        categoriaId: categorias[0]?.id ? String(categorias[0].id) : "1"
      });
      await cargarProductos();
    } catch (err) {
      registrarError("guardar el producto", err);
    }
  };

  // PUT: Actualizar Producto
  const actualizarProducto = async (e) => {
    e.preventDefault();
    if (!productoEditando) return;

    const catId = parseInt(productoEditando.categoriaId, 10);
    const precio = Number(productoEditando.precio);
    if (!productoEditando.nombre.trim() || !Number.isFinite(precio) || precio < 0 || !Number.isInteger(catId)) {
      setFeedback("Completa el nombre, un precio válido y una categoría.");
      return;
    }
    try {
      const authConfig = await getTokenHeaders();
      const payload = {
        nombre: productoEditando.nombre,
        precio,
        categoria: { id: catId }
      };
      const res = await api.put(`/api/admin/productos/${productoEditando.id}`, payload, authConfig);
      setLastStatus(res.status);
      setFeedback("Cambios del producto guardados.");
      setProductoEditando(null);
      await cargarProductos();
    } catch (err) {
      registrarError("actualizar el producto", err);
    }
  };

  // DELETE: Eliminar Producto
  const eliminarProducto = async (id) => {
    if (!window.confirm("¿Estás seguro de que deseas eliminar este producto?")) return;

    try {
      const authConfig = await getTokenHeaders();
      const res = await api.delete(`/api/admin/productos/${id}`, authConfig);
      setLastStatus(res.status);
      setFeedback("Producto eliminado.");
      await cargarProductos();
    } catch (err) {
      registrarError("eliminar el producto", err);
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
    void cargarCategorias();
  }, [cargarCategorias]);

  useEffect(() => {
    if (isAuthenticated) {
      void cargarProductos();
    }
  }, [isAuthenticated, cargarProductos]);

  const productosFiltrados = useMemo(() => {
    const query = busqueda.trim().toLocaleLowerCase("es");
    return productos.filter((producto) => {
      const coincideNombre = !query || producto.nombre?.toLocaleLowerCase("es").includes(query);
      const coincideCategoria = categoriaFiltro === "todas"
        || String(producto.categoria?.id ?? "") === categoriaFiltro;
      return coincideNombre && coincideCategoria;
    });
  }, [busqueda, categoriaFiltro, productos]);

  const valorInventario = productos.reduce((total, producto) => (
    total + (Number(producto.precio) || 0)
  ), 0);
  const visibleFeedback = authError
    ? `Auth0 no pudo completar el inicio de sesión: ${authError.message}`
    : feedback;

  if (isLoading) {
    return (
      <main className="auth-loading">
        <span className="loading-mark"><Icon name="box" size={24} /></span>
        <p>Preparando tu espacio de inventario...</p>
      </main>
    );
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a className="brand" href="#inicio" aria-label="Stock360, inicio">
          <span className="brand-mark"><Icon name="box" size={23} /></span>
          <span className="brand-copy"><strong>stock<span>360</span></strong><small>INVENTORY CLOUD</small></span>
        </a>

        <div className="sidebar-label">GESTIÓN</div>
        <nav className="side-nav" aria-label="Navegación principal">
          <a className="side-link active" href="#inventario"><Icon name="grid" /> <span>Inventario</span></a>
          <a className="side-link" href="#categorias"><Icon name="tag" /> <span>Categorías</span><span className="nav-count">{categorias.length}</span></a>
        </nav>

        <div className="sidebar-bottom">
          <div className="sidebar-footer"><span className="online-dot" /> Sistema operativo</div>
        </div>
      </aside>

      <main className="main-content" id="inicio">
        <header className="topbar">
          <div className="breadcrumb"><span>Workspace</span><Icon name="arrow" size={14} /><strong>Inventario</strong></div>
          <div className="topbar-actions">
            <span className="live-status"><span className="online-dot" /> En línea</span>
            {isAuthenticated ? (
              <div className="user-menu">
                <span className="avatar">{(user?.name || user?.email || "U").charAt(0).toLocaleUpperCase("es")}</span>
                <span className="user-name">{user?.name || user?.email}</span>
                <button className="icon-button logout-button" onClick={handleLogout} disabled={authActionPending} aria-label="Cerrar sesión" title="Cerrar sesión">
                  <Icon name="logout" />
                </button>
              </div>
            ) : (
              <button className="button button-primary login-button" onClick={handleLogin} disabled={authActionPending}>
                <Icon name="lock" size={16} /> {authActionPending ? "Conectando..." : "Iniciar sesión"}
              </button>
            )}
          </div>
        </header>

        <div className="page-content">
          <section className="welcome-row">
            <div>
              <div className="eyebrow"><span className="eyebrow-line" /> PANEL DE CONTROL</div>
              <h1>Tu inventario, <span>en orden.</span></h1>
              <p className="welcome-copy">Una vista clara de todo lo que mueve tu negocio.</p>
            </div>
            <button className="button button-primary" onClick={cargarProductos}>
              <Icon name="refresh" size={17} /> Actualizar datos
            </button>
          </section>

          <BannerSeguridad lastStatus={authError ? null : lastStatus} feedback={visibleFeedback} />

          <section className="stats-grid" aria-label="Resumen del inventario">
            <article className="stat-card">
              <span className="stat-icon stat-icon-purple"><Icon name="box" /></span>
              <div><span className="stat-label">Productos</span><strong className="stat-value">{productos.length}</strong><span className="stat-note">en tu inventario</span></div>
              <span className="stat-decoration">01</span>
            </article>
            <article className="stat-card">
              <span className="stat-icon stat-icon-orange"><Icon name="tag" /></span>
              <div><span className="stat-label">Categorías</span><strong className="stat-value">{categorias.length}</strong><span className="stat-note">grupos activos</span></div>
              <span className="stat-decoration">02</span>
            </article>
            <article className="stat-card">
              <span className="stat-icon stat-icon-green"><Icon name="chart" /></span>
              <div><span className="stat-label">Valor inventario</span><strong className="stat-value">{valorInventario.toLocaleString("es-CL", { style: "currency", currency: "CLP", maximumFractionDigits: 0 })}</strong><span className="stat-note">valor registrado</span></div>
              <span className="stat-decoration">03</span>
            </article>
          </section>

          <section className="panel category-panel" id="categorias">
            <div className="panel-heading">
              <div><div className="section-kicker">ORGANIZACIÓN</div><h2>Categorías</h2><p>Explora tus productos por colección.</p></div>
              <button className="text-button" onClick={cargarCategorias}>Ver todas <Icon name="arrow" size={15} /></button>
            </div>
            <div className="category-list">
              {categorias.map((categoria, index) => (
                <button
                  className={`category-card category-tone-${index % 4}`}
                  key={categoria.id}
                  onClick={() => { setCategoriaFiltro(String(categoria.id)); document.getElementById("inventario")?.scrollIntoView({ behavior: "smooth" }); }}
                >
                  <span className="category-symbol"><Icon name={["box", "tag", "grid", "chart"][index % 4]} size={20} /></span>
                  <span className="category-info"><strong>{categoria.nombre}</strong><small>Colección</small></span>
                  <Icon name="arrow" size={16} />
                </button>
              ))}
              {categorias.length === 0 && <p className="empty-inline">Todavía no hay categorías disponibles.</p>}
            </div>
            {isAuthenticated && (
              <form className="category-add" onSubmit={crearCategoria}>
                <label htmlFor="new-category">¿Necesitas otra categoría?</label>
                <div className="inline-form">
                  <input id="new-category" placeholder="Ej. Accesorios" value={nuevaCategoria} onChange={(e) => setNuevaCategoria(e.target.value)} required />
                  <button className="button button-secondary" type="submit"><Icon name="plus" size={16} /> Agregar</button>
                </div>
              </form>
            )}
          </section>

          <section className="workspace-grid">
            <div className="panel inventory-panel" id="inventario">
              <div className="panel-heading inventory-heading">
                <div><div className="section-kicker">CATÁLOGO</div><h2>Inventario de productos</h2><p>Busca, revisa y administra tus productos.</p></div>
                <div className="inventory-actions">
                  <button className="button button-outline" onClick={cargarProductos}><Icon name="refresh" size={16} /> Cargar productos</button>
                </div>
              </div>

              <div className="table-toolbar">
                <label className="search-box">
                  <Icon name="search" size={17} />
                  <input aria-label="Buscar productos" placeholder="Buscar producto..." value={busqueda} onChange={(e) => setBusqueda(e.target.value)} />
                </label>
                <label className="filter-select">
                  <span className="sr-only">Filtrar por categoría</span>
                  <select value={categoriaFiltro} onChange={(e) => setCategoriaFiltro(e.target.value)}>
                    <option value="todas">Todas las categorías</option>
                    {categorias.map((categoria) => <option key={categoria.id} value={categoria.id}>{categoria.nombre}</option>)}
                  </select>
                </label>
              </div>

              {productoEditando && (
                <div className="edit-panel">
                  <div className="edit-heading"><div><div className="section-kicker">EDICIÓN</div><h3>Editar producto #{productoEditando.id}</h3></div><button type="button" className="icon-button" onClick={() => setProductoEditando(null)} aria-label="Cancelar edición"><Icon name="close" /></button></div>
                  <form className="edit-form" onSubmit={actualizarProducto}>
                    <label>Nombre<input value={productoEditando.nombre} onChange={(e) => setProductoEditando({ ...productoEditando, nombre: e.target.value })} required /></label>
                    <label>Precio<input type="number" min="0" step="0.01" value={productoEditando.precio} onChange={(e) => setProductoEditando({ ...productoEditando, precio: e.target.value })} required /></label>
                    <label>Categoría<select value={productoEditando.categoriaId} onChange={(e) => setProductoEditando({ ...productoEditando, categoriaId: e.target.value })}>{categorias.map((categoria) => <option key={categoria.id} value={categoria.id}>{categoria.nombre}</option>)}</select></label>
                    <button className="button button-primary" type="submit">Guardar cambios</button>
                  </form>
                </div>
              )}

              <div className="inventory-table-wrap">
                {productosFiltrados.length === 0 ? (
                  <div className="empty-state">
                    <span className="empty-icon"><Icon name="box" size={26} /></span>
                    <h3>{productos.length === 0 ? "Tu inventario está listo para empezar" : "No encontramos productos"}</h3>
                    <p>{productos.length === 0 ? "Carga los productos para verlos aquí, o agrega uno nuevo desde el panel lateral." : "Prueba con otra búsqueda o categoría."}</p>
                    {productos.length === 0 && <button className="button button-outline" onClick={cargarProductos}><Icon name="refresh" size={16} /> Cargar inventario</button>}
                    {(busqueda || categoriaFiltro !== "todas") && <button className="clear-filter" onClick={() => { setBusqueda(""); setCategoriaFiltro("todas"); }}>Limpiar filtros</button>}
                  </div>
                ) : (
                  <table className="inventory-table">
                    <thead><tr><th>PRODUCTO</th><th>CATEGORÍA</th><th>PRECIO</th>{isAuthenticated && <th className="actions-column">ACCIONES</th>}</tr></thead>
                    <tbody>
                      {productosFiltrados.map((producto, index) => (
                        <tr key={producto.id}>
                          <td><div className="product-cell"><span className={`product-thumb product-thumb-${index % 4}`}><Icon name="box" size={18} /></span><span><strong>{producto.nombre}</strong><small>SKU-{String(producto.id).padStart(4, "0")}</small></span></div></td>
                          <td><span className="category-badge">{producto.categoria?.nombre || "Sin categoría"}</span></td>
                          <td className="price-cell">{Number(producto.precio).toLocaleString("es-CL", { style: "currency", currency: "CLP" })}</td>
                          {isAuthenticated && <td><div className="row-actions">
                            <button className="row-action edit-action" onClick={() => prepararEdicion(producto)} aria-label={`Editar ${producto.nombre}`} title="Editar"><Icon name="edit" size={16} /></button>
                            <button className="row-action delete-action" onClick={() => eliminarProducto(producto.id)} aria-label={`Eliminar ${producto.nombre}`} title="Eliminar"><Icon name="trash" size={16} /></button>
                          </div></td>}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
              {productos.length > 0 && <div className="table-footer"><span>Mostrando <strong>{productosFiltrados.length}</strong> de <strong>{productos.length}</strong> productos</span><span className="secure-note"><Icon name="lock" size={13} /> Conexión segura</span></div>}
            </div>

            <aside className="side-column">
              <section className="create-card">
                <div className="create-card-top"><span className="create-icon"><Icon name="plus" size={20} /></span><span className="admin-pill"><Icon name="lock" size={12} /> ADMIN</span></div>
                <div className="section-kicker">NUEVO REGISTRO</div>
                <h2>Agregar producto</h2>
                <p className="create-copy">Completa los datos para sumar un producto a tu inventario.</p>
                {isAuthenticated ? (
                  <form className="product-form" onSubmit={crearProducto}>
                    <label htmlFor="product-name">Nombre del producto</label>
                    <input id="product-name" placeholder="Ej. Lámpara de escritorio" value={nuevoProducto.nombre} onChange={(e) => setNuevoProducto({ ...nuevoProducto, nombre: e.target.value })} required />
                    <label htmlFor="product-price">Precio</label>
                    <div className="price-input"><span>$</span><input id="product-price" type="number" min="0" step="0.01" placeholder="0" value={nuevoProducto.precio} onChange={(e) => setNuevoProducto({ ...nuevoProducto, precio: e.target.value })} required /></div>
                    <label htmlFor="product-category">Categoría</label>
                    <select id="product-category" value={nuevoProducto.categoriaId} onChange={(e) => setNuevoProducto({ ...nuevoProducto, categoriaId: e.target.value })} required>
                      <option value="">Seleccione una categoría</option>
                      {categorias.map((categoria) => <option key={categoria.id} value={categoria.id}>{categoria.nombre}</option>)}
                    </select>
                    <button className="button button-primary create-submit" type="submit"><Icon name="plus" size={17} /> Guardar producto</button>
                  </form>
                ) : (
                  <div className="login-prompt">
                    <p><Icon name="lock" size={16} /> Inicia sesión para crear y administrar productos.</p>
                    <button className="button button-primary create-submit" onClick={handleLogin} disabled={authActionPending}>{authActionPending ? "Conectando..." : "Iniciar sesión"} <Icon name="arrow" size={16} /></button>
                  </div>
                )}
                <div className="form-footnote"><span className="online-dot" /> Tus datos se guardan de forma segura</div>
              </section>
            </aside>
          </section>

          <footer className="page-footer"><span>© 2026 Stock360</span><span>Inventario simple. Decisiones inteligentes.</span></footer>
        </div>
      </main>
    </div>
  );
}

export default App;
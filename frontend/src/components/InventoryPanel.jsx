import { useState } from "react";
import { Icon } from "./Icon";

export function InventoryPanel({
  productos,
  productosFiltrados,
  categorias,
  isAuthenticated,
  busqueda,
  setBusqueda,
  categoriaFiltro,
  setCategoriaFiltro,
  cargarProductos,
  productoEditando,
  setProductoEditando,
  actualizarProducto,
  publicarActualizacionStock,
  prepararEdicion,
  eliminarProducto,
}) {
  const [stockObjetivo, setStockObjetivo] = useState({});
  const [productoActualizando, setProductoActualizando] = useState(null);

  const enviarActualizacionStock = async (event, producto) => {
    event.preventDefault();
    setProductoActualizando(producto.id);
    try {
      await publicarActualizacionStock(
        producto.id,
        stockObjetivo[producto.id] ?? String(producto.stock ?? 0)
      );
    } finally {
      setProductoActualizando(null);
    }
  };

  return (
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
          <input aria-label="Buscar productos" placeholder="Buscar producto..." value={busqueda} onChange={(event) => setBusqueda(event.target.value)} />
        </label>
        <label className="filter-select">
          <span className="sr-only">Filtrar por categoría</span>
          <select value={categoriaFiltro} onChange={(event) => setCategoriaFiltro(event.target.value)}>
            <option value="todas">Todas las categorías</option>
            {categorias.map((categoria) => <option key={categoria.id} value={categoria.id}>{categoria.nombre}</option>)}
          </select>
        </label>
      </div>

      {productoEditando && (
        <div className="edit-panel">
          <div className="edit-heading"><div><div className="section-kicker">EDICIÓN</div><h3>Editar producto #{productoEditando.id}</h3></div><button type="button" className="icon-button" onClick={() => setProductoEditando(null)} aria-label="Cancelar edición"><Icon name="close" /></button></div>
          <form className="edit-form" onSubmit={actualizarProducto}>
            <label>Nombre<input value={productoEditando.nombre} onChange={(event) => setProductoEditando({ ...productoEditando, nombre: event.target.value })} required /></label>
            <label>Precio<input type="number" min="0" step="0.01" value={productoEditando.precio} onChange={(event) => setProductoEditando({ ...productoEditando, precio: event.target.value })} required /></label>
            <label>Stock mínimo<input type="number" min="0" step="1" value={productoEditando.stockMinimo} onChange={(event) => setProductoEditando({ ...productoEditando, stockMinimo: event.target.value })} required /></label>
            <label>Categoría<select value={productoEditando.categoriaId} onChange={(event) => setProductoEditando({ ...productoEditando, categoriaId: event.target.value })}>{categorias.map((categoria) => <option key={categoria.id} value={categoria.id}>{categoria.nombre}</option>)}</select></label>
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
            <thead><tr><th>PRODUCTO</th><th>CATEGORÍA</th><th>PRECIO</th><th>STOCK</th>{isAuthenticated && <th className="actions-column">ACCIONES</th>}</tr></thead>
            <tbody>
              {productosFiltrados.map((producto, index) => (
                <tr key={producto.id}>
                  <td><div className="product-cell"><span className={`product-thumb product-thumb-${index % 4}`}><Icon name="box" size={18} /></span><span><strong>{producto.nombre}</strong><small>SKU-{String(producto.id).padStart(4, "0")}</small></span></div></td>
                  <td><span className="category-badge">{producto.categoria?.nombre || "Sin categoría"}</span></td>
                  <td className="price-cell">{Number(producto.precio).toLocaleString("es-CL", { style: "currency", currency: "CLP" })}</td>
                  <td>
                    {(() => {
                      const stock = producto.stock ?? 0;
                      const stockMinimo = producto.stockMinimo ?? 5;
                      const critico = stock <= stockMinimo;
                      return (
                        <div className="stock-cell">
                          <strong>{stock}</strong>
                          <span>Mínimo: {stockMinimo}</span>
                          <span className={`stock-status ${critico ? "stock-status-critical" : "stock-status-normal"}`}>
                            {critico ? "Crítico" : "Normal"}
                          </span>
                        </div>
                      );
                    })()}
                  </td>
                  {isAuthenticated && <td>
                    <div className="inventory-row-controls">
                      <form className="stock-update-form" onSubmit={(event) => enviarActualizacionStock(event, producto)}>
                        <label className="sr-only" htmlFor={`stock-update-${producto.id}`}>Nuevo stock para {producto.nombre}</label>
                        <input
                          id={`stock-update-${producto.id}`}
                          type="number"
                          min="0"
                          step="1"
                          value={stockObjetivo[producto.id] ?? String(producto.stock ?? 0)}
                          onChange={(event) => setStockObjetivo({
                            ...stockObjetivo,
                            [producto.id]: event.target.value,
                          })}
                          aria-label={`Nuevo stock para ${producto.nombre}`}
                        />
                        <button className="button button-secondary" type="submit" disabled={productoActualizando === producto.id}>
                          {productoActualizando === producto.id ? "Enviando..." : "Actualizar stock"}
                        </button>
                      </form>
                      <div className="row-actions">
                        <button className="row-action edit-action" onClick={() => prepararEdicion(producto)} aria-label={`Editar ${producto.nombre}`} title="Editar"><Icon name="edit" size={16} /></button>
                        <button className="row-action delete-action" onClick={() => eliminarProducto(producto.id)} aria-label={`Eliminar ${producto.nombre}`} title="Eliminar"><Icon name="trash" size={16} /></button>
                      </div>
                    </div>
                  </td>}
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
      {productos.length > 0 && <div className="table-footer"><span>Mostrando <strong>{productosFiltrados.length}</strong> de <strong>{productos.length}</strong> productos</span><span className="secure-note"><Icon name="lock" size={13} /> Conexión segura</span></div>}
    </div>
  );
}

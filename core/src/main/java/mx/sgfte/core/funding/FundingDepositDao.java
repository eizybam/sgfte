package mx.sgfte.core.funding;

import mx.sgfte.core.shared.db.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC plano sobre funding_deposit. Tonto a propósito: no decide si un depósito
 * se acepta —eso es FundingService— sólo lo escribe y lo lee.
 *
 * insert() recibe la Connection del Service porque el depósito, el abono al
 * saldo y el asiento del ledger tienen que caer juntos o no caer.
 */
public class FundingDepositDao {

    /**
     * Inserta el depósito dentro de la transacción de quien llama y devuelve su
     * id, que es lo que el asiento del ledger va a necesitar para apuntarle.
     */
    public long insert(Connection conn, FundingDeposit d) throws SQLException {
        String sql = "INSERT INTO funding_deposit "
                   + "(canal, referencia, ordenante_nombre, ordenante_rfc, ordenante_clabe, "
                   + " institucion, sucursal, beneficiario_clabe, monto, concepto, "
                   + " referencia_numerica, fecha_operacion) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, new String[] { "id" })) {
            ps.setString(1, d.getCanal());
            ps.setString(2, d.getReferencia());
            ps.setString(3, d.getOrdenanteNombre());
            ps.setString(4, d.getOrdenanteRfc());
            ps.setString(5, d.getOrdenanteClabe());
            ps.setString(6, d.getInstitucion());
            ps.setString(7, d.getSucursal());
            ps.setString(8, d.getBeneficiarioClabe());
            ps.setBigDecimal(9, d.getMonto());
            ps.setString(10, d.getConcepto());
            if (d.getReferenciaNumerica() == null) {
                ps.setNull(11, java.sql.Types.NUMERIC);
            } else {
                ps.setInt(11, d.getReferenciaNumerica());
            }
            ps.setTimestamp(12, Timestamp.valueOf(d.getFechaOperacion()));
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new IllegalStateException("No se obtuvo el id del depósito");
                return keys.getLong(1);
            }
        }
    }

    /**
     * ¿Ya habíamos registrado esta referencia?
     *
     * La garantía de verdad es el UNIQUE de la base — esto sólo existe para
     * poder devolver "referencia repetida" en vez de un ORA-00001 en la cara
     * del usuario. Si dos peticiones entran a la vez, gana el índice, y para
     * eso FundingService también atrapa la violación al insertar.
     */
    public boolean exists(String referencia) {
        String sql = "SELECT 1 FROM funding_deposit WHERE referencia = ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, referencia);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando la referencia del depósito", e);
        }
    }

    /** Los últimos depósitos recibidos, para el panel de la Concentradora. */
    public List<FundingDeposit> findRecent(int limit) {
        String sql = "SELECT id, canal, referencia, ordenante_nombre, ordenante_rfc, "
                   + "       ordenante_clabe, institucion, sucursal, beneficiario_clabe, "
                   + "       monto, concepto, referencia_numerica, fecha_operacion, recibido_en "
                   + "  FROM funding_deposit "
                   + " ORDER BY fecha_operacion DESC, id DESC FETCH FIRST ? ROWS ONLY";
        List<FundingDeposit> list = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(read(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Error leyendo los depósitos recibidos", e);
        }
    }

    /** Cuántos van, para el resumen. */
    public int count() {
        try (Connection c = Db.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM funding_deposit")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error contando los depósitos", e);
        }
    }

    private FundingDeposit read(ResultSet rs) throws SQLException {
        FundingDeposit d = new FundingDeposit();
        d.setId(rs.getLong("id"));
        d.setCanal(rs.getString("canal"));
        d.setReferencia(rs.getString("referencia"));
        d.setOrdenanteNombre(rs.getString("ordenante_nombre"));
        d.setOrdenanteRfc(rs.getString("ordenante_rfc"));
        d.setOrdenanteClabe(trim(rs.getString("ordenante_clabe")));
        d.setInstitucion(rs.getString("institucion"));
        d.setSucursal(rs.getString("sucursal"));
        d.setBeneficiarioClabe(trim(rs.getString("beneficiario_clabe")));
        d.setMonto(rs.getBigDecimal("monto"));
        d.setConcepto(rs.getString("concepto"));
        int ref = rs.getInt("referencia_numerica");
        d.setReferenciaNumerica(rs.wasNull() ? null : ref);
        Timestamp op = rs.getTimestamp("fecha_operacion");
        d.setFechaOperacion(op == null ? null : op.toLocalDateTime());
        Timestamp rec = rs.getTimestamp("recibido_en");
        d.setRecibidoEn(rec == null ? null : rec.toLocalDateTime());
        return d;
    }

    /** Las CLABE son CHAR(18): Oracle las devuelve rellenadas si algo entró corto. */
    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}

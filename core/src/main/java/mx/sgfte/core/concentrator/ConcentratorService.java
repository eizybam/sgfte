package mx.sgfte.core.concentrator;

/** Concentrator logic. Fondear ya no vive aquí: ver FundingService (V12). */
public class ConcentratorService {

    private final ConcentratorDao dao;

    public ConcentratorService() {
        this(new ConcentratorDao());
    }

    public ConcentratorService(ConcentratorDao dao) {
        this.dao = dao;
    }

    public ConcentratorAccount getConcentrator() {
        return dao.findSingleton();
    }

    /*
      Aquí vivía fund(monto, actor): validaba que el monto fuera positivo y
      subía el saldo. Se eliminó en V12 y no se sustituyó por nada dentro de
      esta clase.

      El motivo es que era la única operación del sistema que creaba dinero sin
      respaldo: un monto positivo era todo lo que hacía falta. Mientras el
      método siguiera existiendo, la puerta seguiría abierta para el siguiente
      que la llamara. Fondear ahora es FundingService.register(deposito), que
      no se puede invocar sin una referencia bancaria.
     */
}

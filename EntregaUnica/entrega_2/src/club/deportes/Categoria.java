package club.deportes;

import java.time.LocalDate;
import java.time.Period;

public enum Categoria {

    SUB13,
    SUB15,
    SUB17,
    MAYORES;

    public boolean admiteEdad(LocalDate fechaNacimiento) {

        int edad = Period.between(fechaNacimiento, LocalDate.now()).getYears();

        switch (this) {

            case SUB13:
                return edad >= 0 && edad < 13;

            case SUB15:
                return edad >= 13 && edad < 15;

            case SUB17:
                return edad >= 15 && edad < 17;

            case MAYORES:
                return edad >= 17;

            default:
                return false;
        }
    }
}
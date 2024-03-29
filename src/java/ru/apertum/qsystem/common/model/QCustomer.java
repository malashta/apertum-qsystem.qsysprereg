/*
 *  Copyright (C) 2010 {Apertum}Projects. web: www.apertum.ru email: info//@apertum.ru
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ru.apertum.qsystem.common.model;

import java.io.Serializable;
import java.util.LinkedList;

import ru.apertum.qsystem.server.model.QService;
import ru.apertum.qsystem.server.model.QUser;
import ru.apertum.qsystem.server.model.results.QResult;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.util.Date;
import ru.apertum.qsystem.common.CustomerState;


/**
 * //@author Evgeniy Egorov
 * Реализация клиета
 * Наипростейший "очередник".
 * Используется для организации простой очереди.
 * Если используется СУБД, то сохранение происходит при смене ссостояния.
 * ВАЖНО! Всегда изменяйте статус кастомера при его изменении, особенно при его удалении.
 * 
 */

public final class QCustomer implements Comparable<QCustomer>, Serializable {

    /**
     * создаем клиента имея только его номер в очереди. Префикс не определен, т.к. еще не знаем об услуге
     * куда его поставить. Присвоем кастомену услугу - присвоются и ее атрибуты.
     * //@param number номер клиента в очереди
     */
    public QCustomer(int number) {
        this.number = number;
        id = new Date().getTime();
        setStandTime(new Date()); // действия по инициализации при постановке
        // все остальные всойства кастомера об услуге куда попал проставятся в самой услуге при помещении кастомера в нее
        //QLog.l().logger().debug("Создали кастомера с номером " + number);
    }
    @Expose
    @SerializedName("id")
    private Long id = new Date().getTime();

    //@Id
    //@Column(name = "id")
    ////@GeneratedValue(strategy = GenerationType.AUTO) простаяляем уникальный номер времени создания.
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    /**
     *  АТРИБУТЫ "ОЧЕРЕДНИКА"
     *  персональный номер, именно по нему система ведет учет и управление очередниками
     * //@param number новер - целое число
     */
    @Expose
    @SerializedName("number")
    private Integer number;

    public void setNumber(Integer number) {
        this.number = number;
    }

    //@Column(name = "number")
    public int getNumber() {
        return number;
    }
    /**
     * АТРИБУТЫ "ОЧЕРЕДНИКА"
     *  состояние кастомера, именно по нему система знает что сейчас происходит с кастомером
     * Это состояние менять только если кастомер уже готов к этому и все другие параметры у него заполнены.
     * Если данные пишутся в БД, то только по состоянию завершенности обработки над ним.
     * Так что если какая-то итерация закончена и про кастомера должно занестись в БД, то как и надо выставлять что кастомер ЗАКОНЧИЛ обрабатываться,
     * а уж потом менять , если надо, его атрибуты и менять состояние, например на РЕДИРЕКТЕННОГО.
     * //@param state - состояние клиента
     * //@see ru.apertum.qsystem.common.Uses
     */
    @Expose
    @SerializedName("state")
    private CustomerState state;

    public void setState(CustomerState state) {
        setState(state, new Long(-1));
    }

    /**
     * Специально для редиректа и возврата после редиректа
     * //@param state
     * //@param newServiceId 
     */
    public void setState(CustomerState state, Long newServiceId) {

        this.state = state;

       
    }


    //@Transient
    public CustomerState getState() {
        return state;
    }
    /**
     *  ПРИОРИТЕТ "ОЧЕРЕДНИКА"
     */
    @Expose
    @SerializedName("priority")
    private Integer priority;

    public void setPriority(int priority) {
        this.priority = priority;
    }

    //@Transient
    public IPriority getPriority() {
        return new Priority(priority);
    }

    /**
     *  Сравнение очередников для выбора первого. Участвует приоритет очередника.
     *  сравним по приоритету, потом по времени
     * //@param customer
     * //@return используется отношение "обслужится позднее"(сравнение дает ответ на вопрос "я обслужусь позднее чем тот в параметре?")
     *         1 - "обслужится позднее" чем кастомер в параметре, -1 - "обслужится раньше"  чем кастомер в параметре, 0 - одновременно
     *         -1 - быстрее обслужится чем кастомер из параметров, т.к. встал раньше
     *         1 - обслужится после чем кастомер из параметров, т.к. встал позднее
     */
    //@Override
    public int compareTo(QCustomer customer) {
        int resultCmp = -1 * getPriority().compareTo(customer.getPriority()); // (-1) - т.к.  больший приоритет быстрее обслужится

        if (resultCmp == 0) {
            if (this.getStandTime().before(customer.getStandTime())) {
                resultCmp = -1;
            } else if (this.getStandTime().after(customer.getStandTime())) {
                resultCmp = 1;
            }
        }
        if (resultCmp == 0) {
            //QLog.l().logger().warn("Клиенты не могут быть равны.");
            resultCmp = -1;
        }
        return resultCmp;
    }
    /**
     * К какой услуге стоит. Нужно для статистики.
     */
    @Expose
    @SerializedName("to_service")
    private QService service;

    //@ManyToOne(fetch = FetchType.LAZY)
    //@JoinColumn(name = "service_id")
    public QService getService() {
        return service;
    }

    /**
     * Кастомеру проставим атрибуты услуги включая имя, описание, префикс. 
     * Причем префикс ставится раз и навсегда.
     * При добавлении кастомера в услугу addCustomer() происходит тоже самое + выставляется префикс, если такой
     * атрибут не добавлен в XML-узел кастомера
     * //@param service не передавать тут NULL
     */
    public void setService(QService service) {
        this.service = service;
        // Префикс для кастомера проставится при его создании, один раз и на всегда.
        if (getPrefix() == null) {
            setPrefix(service.getPrefix());
        }
        //QLog.l().logger().debug("Клиента \"" + getPrefix() + getNumber() + "\" поставили к услуге \"" + service.getName() + "\"");
    }
    /**
     * Результат работы с пользователем
     */
    private QResult result;

    //@ManyToOne(fetch = FetchType.LAZY)
    //@JoinColumn(name = "result_id")
    public QResult getResult() {
        return result;
    }

    public void setResult(QResult result) {
        this.result = result;
        if (result == null) {
            //QLog.l().logger().debug("Обозначать результат работы с кастомером не требуется");
        } else {
            //QLog.l().logger().debug("Обозначили результат работы с кастомером: \"" + result.getName() + "\"");
        }
    }
    /**
     * Кто его обрабатывает. Нужно для статистики.
     */
    
    private QUser user;

    
    public QUser getUser() {
        return user;
    }

    public void setUser(QUser user) {
        this.user = user;
        //QLog.l().logger().debug("Клиенту \"" + getPrefix() + getNumber() + (user == null ? " юзера нету, еще он его не вызывал\"" : " опредилили юзера \"" + user.getName() + "\""));
    }
    /**
     * Префикс услуги, к которой стоит кастомер.
     * //@return Строка префикса.
     */
    @Expose
    @SerializedName("prefix")
    private String prefix;

    //@Column(name = "service_prefix")
    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix == null ? "" : prefix;
    }
    @Expose
    @SerializedName("stand_time")
    private Date standTime;

    //@Column(name = "stand_time")
    //@Temporal(TemporalType.TIMESTAMP)
    public Date getStandTime() {
        return standTime;
    }

    public void setStandTime(Date date) {
        this.standTime = date;
    }
    @Expose
    @SerializedName("start_time")
    private Date startTime;

    //@Column(name = "start_time")
    //@Temporal(TemporalType.TIMESTAMP)
    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date date) {
        this.startTime = date;
    }
    private Date callTime;

    public void setCallTime(Date date) {
        this.callTime = date;
    }

    //@Transient
    public Date getCallTime() {
        return callTime;
    }
    @Expose
    @SerializedName("finish_time")
    private Date finishTime;

    //@Column(name = "finish_time")
    //@Temporal(TemporalType.TIMESTAMP)
    public Date getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Date date) {
        this.finishTime = date;
    }
    @Expose
    @SerializedName("input_data")
    private String input_data = "";

    /**
     * Введенные кастомером данные на пункте регистрации.
     * //@return
     */
    //@Column(name = "input_data")
    public String getInput_data() {
        return input_data;
    }

    public void setInput_data(String input_data) {
        this.input_data = input_data;
    }
    /**
     * Список услуг в которые необходимо вернуться после редиректа
     * Новые услуги для возврата добвляются в начало списка.
     * При возврате берем первую из списка и удаляем ее.
     */
    private final LinkedList<QService> serviceBack = new LinkedList<QService>();

    /**
     * При редиректе если есть возврат. то добавим услугу для возврата
     * //@param service в эту услугу нужен возврат
     */
    public void addServiceForBack(QService service) {
        serviceBack.addFirst(service);
        needBack = !serviceBack.isEmpty();
    }

    /**
     * Куда вернуть если работу закончили но кастомер редиректенный
     * //@return вернуть в эту услугу
     */
    //@Transient
    public QService getServiceForBack() {
        needBack = serviceBack.size() > 1;
        return serviceBack.pollFirst();
    }
    @Expose
    @SerializedName("need_back")
    private boolean needBack = false;

    public boolean needBack() {
        return needBack;
    }
    /**
     * Комментариии юзеров о кастомере при редиректе и отправки в отложенные
     */
    @Expose
    @SerializedName("temp_comments")
    private String tempComments = "";

    //@Transient
    public String getTempComments() {
        return tempComments;
    }

    public void setTempComments(String tempComments) {
        this.tempComments = tempComments;
    }
    /**
     *
     */
    @Expose
    @SerializedName("post_atatus")
    private String postponedStatus = "";

    //@Transient
    public String getPostponedStatus() {
        return postponedStatus;
    }

    public void setPostponedStatus(String postponedStatus) {
        this.postponedStatus = postponedStatus;
    }

    /**
     * Вернет XML-строку, описывающую кастомера
     */
    //@Override
    public String toString() {
        return prefix + getNumber() + " " + postponedStatus;
    }
}

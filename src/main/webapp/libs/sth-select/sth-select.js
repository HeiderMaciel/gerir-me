"use strict";

/**
 * SthOverlay should be an external component. Many components 
 * created by us uses a type of overlay, and it should be added 
 * to the DOM only once.
 */

(function () {

    function SthOverlay() {

        var _$overlay = null;

        /**
   * Constructor.
   * 
   * Creates the overlay only once.
   */
        (function create() {

            if (_isAlreadyCreated()) {
                _$overlay = $(".sth-overlay");
                return;
            }

            _$overlay = $('<div class="sth-overlay"></div>');
            _$overlay.appendTo($("body"));
        })();

        /**
           * Checks if overlay is already inserted on the DOM.
           */
        function _isAlreadyCreated() {
            var alreadyExistent = $(".sth-overlay");
            return alreadyExistent && alreadyExistent.length > 0;
        }

        /**
   * Shows the overlay.
   */
        function show(values) {
            _$overlay.fadeIn(500);
        }

        /**
   * Hides the overlay.
   */
        function hide() {
            _$overlay.fadeOut(500);
        }

        return {
            show: show,
            hide: hide
        };
    }

    window.SthOverlay = window.SthOverlay || SthOverlay;
})();
"use strict";

(function () {

    function SthSelectPopup(properties) {

        var self = this;
        var _$popup = null;
        var _$title = null;
        var _$titleText = null;
        var _$titleClose = null;
        var _$content = null;
        var _$filter = null;
        var _$overlay = null;
        var _$select = null;
        var _properties = properties;
        var _onSelectCallback = null;
        var _qntityOfItems = 0;
        var _items = [];
        var _filteredItems = [];
        var _selectedItemIndex = -1;
        var _isOpen = false;

        /**
   * Max of height (in pixels) that the popup can 
   * assume when open.
   */
        var MAX_HEIGHT = 500;

        /**
   * Constructor.
   * Creates the popup section element in the DOM.
   * 
   * The section is created only once. Several calls 
   * does not have effect.
   */
        (function create() {

            if (isAlreadyInDOM()) {
                _$popup = $(".sth-select-popup");
                _$title = $(".sth-select-title");
                _$titleText = $(".sth-select-title-text");
                _$titleClose = $(".sth-select-title-close");
                _$content = $(".sth-select-content");
                _$filter = $(".sth-select-filter");
                _$overlay = $(".sth-overlay");
            } else {
                _$popup = $('<section class="sth-select-popup"></section>');
                _$title = $('<div class="sth-select-title"></div>');
                _$titleText = $('<span class="sth-select-title-text"></span>');
                _$titleClose = $('<span class="sth-select-title-close">X</span>');
                _$content = $('<div class="sth-select-content"></div>');
                _$filter = $('<input class="sth-select-filter"/>');
                _$overlay = new window.SthOverlay();

                $(document).keydown(function(e){
                    if(e.which === 38){ // arrow down
                        _selectElementAbove();
                    } else if(e.which === 40){ // arrow up
                        _selectElementBelow();
                    } else if(e.which === 13){ // enter
                        _selectElement();
                    }
                });

                _$title.append(_$titleText).append(_$titleClose);
                _$popup.append(_$title).append(_$filter).append(_$content).appendTo($("body"));
            }

            _$titleClose.click(function (e) {
                hide();
            });

            $(document).keydown(function (e) {
                if(e.keyCode == 27) // ESC
                    hide();
            });

            _$filter.keydown(function (e) {
                var BACKSPACE = 8;
                var SPACE = 32;
                var ZERO = 49;
                var Z = 90;
                var keyCode = e.keyCode;

                if( (keyCode >= ZERO && keyCode <= Z) || (keyCode == SPACE || keyCode == BACKSPACE) )
                    setTimeout(function(){
                        _renderList();
                    });
            });

            _$select = properties.select;
            _items = properties.items;
            _filteredItems = _items;
            _qntityOfItems = _items.length;
        })();

        /**
   * Checks if the popup is already inserted in DOM.
   * It prevents many insertions and performance loss.
   */
        function isAlreadyInDOM() {
            var $alreadyExistent = $(".sth-select-popup");
            return $alreadyExistent && $alreadyExistent.length > 0;
        }

        /**
   * Shows the popup on the screen.
   */
        function show(values) {
            _isOpen = true;
            _selectedItemIndex = -1;

            _$overlay.show();

            if(_$filter && _$filter.length > 0) _$filter.val("");

            if (values && Array.isArray(values)){
                _items = values;
                _qntityOfItems = values.length;
            }

            // Apply title to the popup
            _$titleText.text(_properties.title);

            // Defines if filter is available or not
            _controlFilterVisibility();

            // Renders all elements in the list
            _renderList();

            _$popup.animate({ height: _calcMaxHeight() }, 500);
        }

        function _setFirstItemAsSelected(){
            if(_qntityOfItems > 0){
                _selectedItemIndex = 0; 
                var items = $('.sth-select-item').toArray();
                $(items[_selectedItemIndex]).addClass("selected");
            }
        }

        function _selectElementAbove(){
            if(!_isOpen) return false;

            if(_selectedItemIndex > 0){
                var items = $('.sth-select-item').toArray();
                $(items[_selectedItemIndex]).removeClass("selected");

                _selectedItemIndex--;
                $(items[_selectedItemIndex]).addClass("selected");
            }
        }

        function _selectElementBelow(){
            if(!_isOpen) return false;

            if(_selectedItemIndex < _qntityOfItems - 1){
                var items = $('.sth-select-item').toArray();
                $(items[_selectedItemIndex]).removeClass("selected");

                _selectedItemIndex++;
                $(items[_selectedItemIndex]).addClass("selected");
            }
        }

        function _selectElement(){
            hide();
            if(_filteredItems.length < 1)
                _filteredItems = _items;

            var item = _filteredItems[_selectedItemIndex];
            _onSelectCallback(item);
        }

        /**
   * Calculates pop-up's height based on 
   * number of added items.
   */
        /**
         * Calculates the max allowed popup height, based on available
         * screen space.
         * This is useful mainly when mobile's keyboard is open.
         */
        function _calcMaxHeight() {
            // Get the height of the popup
            var singleItemHeight = _$content.find(".sth-select-item").first().outerHeight();
            var allItemsHeight = singleItemHeight * _qntityOfItems;
            var titleHeight = _$title.outerHeight();
            var filterHeight = _$filter.outerHeight();
            var finalPopupHeight = (allItemsHeight + titleHeight + filterHeight);

            // If is bigger than height available on document,
            // make it equals to available space
            var availableHeight = $(window).outerHeight();
            if(availableHeight < finalPopupHeight){
                finalPopupHeight = availableHeight;
            } else if(finalPopupHeight > MAX_HEIGHT){
                finalPopupHeight = MAX_HEIGHT;
            }

            return finalPopupHeight;
        }

        $(window).resize(function(){
            if(!_isOpen) return;

            // Timeout because the popup shows in an animation
            setTimeout(function(){
                var popupHeight = _calcMaxHeight();
                _$popup.outerHeight(popupHeight);
            }, 500);
        });

        /*
         * Hides the popup on the screen.
         */
        function hide() {
            _isOpen = false;
            _$overlay.hide();
            _$popup.animate({ height: 0 }, 500);
        }

        /*
         * Add an item.
         */
        function _addItem(item, autoRender) {
            autoRender = autoRender || true;

            var text = item.text;
            var $listItem = $('<div class="sth-select-item">' + text + '</div>');

            if (autoRender) _$content.append($listItem);

            return $listItem;
        }

        /**
   * Renders all elements in the list of options.
   */
        function _renderList() {
            _clear();

            var rerenderOnEachItem = false;
            var $listItems = $([]);
            var textFilter = _$filter.val().toLowerCase();

            _filteredItems = _items.filter(function (item) {
                if (item.lowerCasedText.indexOf(textFilter) != -1) {
                    var $listItem = _addItem(item, rerenderOnEachItem);
                    $listItem.click(function () {
                        _onSelectCallback(item);
                        hide();
                    });

                    $listItems = $listItems.add($listItem);
                    return true;
                }
            });

            _setFirstItemAsSelected();

            _$content.append($listItems);

            var popupHeight = _calculatePopupHeight();
            var titleHeight = _$title.outerHeight();
            var filterHeight = _$filter.outerHeight();
            _$content.outerHeight(popupHeight - titleHeight - filterHeight);
        }

        /**
   * Clear (removes from DOM) all elements on the list.
   */
        function _clear() {
            _$content.empty();
        }

        /**
   * Event handler which calls a callback when an item 
   * is selected.
   */
        function onSelect(callback) {
            _onSelectCallback = callback;
        }

        /**
   * Sets the filter field visibility based on 
   * hasFilter property.
   */
        function _controlFilterVisibility() {
            var visibility = _properties.hasFilter ? "block" : "none";
            _$filter.css("display", visibility);
            _$filter.attr("placeholder", _properties.filterPlaceholder);

            if(_properties.hasFilter)
                _$filter.focus();
        }

        /**
   * Public available methods.
   */
        return {
            show: show,
            hide: hide,
            onSelect: onSelect
        };
    }

    window.SthSelect = window.SthSelect || {};
    window.SthSelect.SthSelectPopup = SthSelectPopup;
})();
"use strict";

/*
 * Dependencies
 */

var $ = window.jQuery;

/*
 * Constructor
 */
(function () {

    $.fn.SthSelect = function SthSelect(properties) {

        if(!properties)
            return this;

        var _$self = $(this);
        var _$originalSelect = null;
        var _$popup = null;
        var _$fakeSelect = null;
        var _properties = {};
        var _values = [];

        (function initialize($this) {
            _$originalSelect = $this;
            _properties = buildDefault(properties);
            _values = extractValues($this);
            _$fakeSelect = fudgeSelect($this, properties);

            var popupProperties = {
                items: _values,
                title: _properties.title,
                hasFilter: _properties.filter,
                filterPlaceholder: _properties.filterPlaceholder,
                select: _$originalSelect
            };
            _$popup = new window.SthSelect.SthSelectPopup(popupProperties);

            _$popup.onSelect(applySelectedValue);
            _$fakeSelect.click(openPopup);
        })($(this));

        function buildDefault(properties) {
            return $.extend({
                title: "Select an option",
                placeholder: "Choose an option",
                autoSize: false,
                filter: false,
                filterPlaceholder: "Search"
            }, properties);
        }

        function extractValues($this) {
            var values = [];
            $this.find("option").each(function () {
                var $option = $(this);
                var content = { 
                    value: $option.val(), 
                    text: $option.text(), 
                    lowerCasedText: $option.text().toLowerCase() 
                };
                values.push(content);
            });

            return values;
        }

        function fudgeSelect($select, properties) {
            $select.hide();

            var $fakeSelect = $('<div class="sth-select"></div>');
            var $fakeSelectText = $('<span class="sth-select-text"></span>');
            var $fakeSelectArrow = $('<span class="sth-select-arrow"></span>');

            $fakeSelectText.text(properties.placeholder);
            $fakeSelect.append($fakeSelectText);
            $fakeSelect.append($fakeSelectArrow);

            if (!properties.autoSize) $fakeSelect.addClass("fixed-width");

            $select.after($fakeSelect);

            return $fakeSelect;
        }

        function openPopup() {
            _$popup.show(extractValues(_$self));
        }

        function applySelectedValue(selectedValue) {
            var value = selectedValue.value;
            _$originalSelect.val(value);

            var text = selectedValue.text;
            _$fakeSelect.find(".sth-select-text").text(text);

            setTimeout(function(){
                _$originalSelect.trigger('SthSelect:afterSelect');
            }, 650);
        }

        function clearValue() {
            _$originalSelect.val(null);
            _$fakeSelect.find(".sth-select-text").text(_properties.placeholder);
        }

        return {
            clearValue: clearValue
        }
    };

    window.SthSelect = window.SthSelect || {};
    window.SthSelect.init = window.SthSelect.init || SthSelect;
})();

/*
 * Load all elements which use the component by HTML attributes API
 */
$(document).ready(function loadFromHtmlAPI() {

    var $elements = $("select[sth-select]");

    $elements.each(function () {
        var $element = $(this);
        var title = $element.attr("sth-select-title");
        var placeholder = $element.attr("sth-select-placeholder");
        var autoSize = $element.attr("sth-select-autosize");
        var filter = $element.attr("sth-select-filter");
        var filterPlaceholder = $element.attr("sth-select-filter-placeholder");

        $element.SthSelect({
            title: title,
            placeholder: placeholder,
            autoSize: boolFromString(autoSize),
            filter: boolFromString(filter),
            filterPlaceholder: filterPlaceholder
        });
    });

    function boolFromString(string) {
        return string == "true";
    }
});